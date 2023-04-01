package org.ek9lang.compiler.symbol;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.symbol.support.SymbolChecker;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.threads.SharedThreadContext;
import org.ek9lang.core.utils.Holder;

/**
 * This is a very special scope, because the same 'module name' can be defined in
 * multiple files. So when looking to resolve variables we need to look up the
 * scope tree back to the global (program) scope - but when we get here to this module
 * scope we need to look across all the other modules scopes (with the same module name)
 * to check in those as well.
 * So 'resolve' -> 'resolveInThisModuleOnly' or 'resolveWithEnclosingScope'
 * Then 'resolveInThisModuleOnly' -> scopes * 'resolveInThisScopeOnly' for all in scope in module
 * Also 'resolveInThisModuleOnly' -> scopes * 'resolveReferenceInThisScopeOnly' for all scopes in module
 * Finally 'resolveWithEnclosingScope' -> delegates to 'program' 'resolveByFullyQualifiedSearch'
 * And 'resolveWithEnclosingScope' -> delegates to 'program' 'resolveFromImplicitScopes' (org.ek9.lang etc.).
 */
public class ModuleScope extends SymbolTable {

  private final SharedThreadContext<CompilableProgram> compilableProgramContext;

  /**
   * This is where we store the references to other module symbols but as a shorthand.
   * i.e "Item" will map to com.abc.Item.
   */
  private final Map<String, ISymbol> referencesScope = new TreeMap<>();

  /**
   * When a reference to a symbol is made, keep track of where that first reference was.
   * This enables the compiler to give better error messages, indicating where a reference was first successful.
   * So if there are duplicates of clashes, it can report where the first reference was.
   */
  private final Map<String, Token> originalReferenceResolution = new TreeMap<>();

  public ModuleScope(String scopeName, SharedThreadContext<CompilableProgram> context) {
    super(scopeName);
    this.compilableProgramContext = context;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return (o instanceof ModuleScope that)
        && super.equals(o)
        && referencesScope.equals(that.referencesScope);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + referencesScope.hashCode();
    return result;
  }

  /**
   * Add a reference to another construct in another module, so it can be used in shorthand form
   * in this module.
   */
  public void defineReference(final Token token, final ISymbol symbol) {
    AssertValue.checkNotNull("Token cannot be null", token);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    var shortName = ISymbol.getUnqualifiedName(symbol.getName());
    AssertValue.checkFalse("Duplicate reference bing added", referencesScope.containsKey(shortName));
    referencesScope.put(shortName, symbol);
    originalReferenceResolution.put(shortName, token);
  }

  /**
   * Returns the original location a reference was made (if present).
   */
  public Optional<Token> getOriginalReferenceLocation(SymbolSearch search) {
    var shortName = ISymbol.getUnqualifiedName(search.getName());
    return Optional.ofNullable(originalReferenceResolution.get(shortName));
  }

  /**
   * Used when resolving or needing to define a parameterised type, like 'List of String' for example.
   * Now while this moduleScope maybe for the code being parsed and processed, the reference to
   * a generic type like org.ek9.lang::List means that the resulting new type:
   * 'org.ek9.lang::List of org.ek9.lang::String' or 'org.ek9.lang::List of my.mod.area::Widget' will be
   * stored in the module space of the Generic Type.
   * So do not assume this new type will reside in this module scope, it most probably won't.
   */
  public Optional<ISymbol> resolveOrDefine(final PossibleGenericSymbol parameterisedSymbol) {
    Holder<ISymbol> holder = new Holder<>();
    compilableProgramContext.accept(compilableProgram -> {
      var result = compilableProgram.resolveOrDefine(parameterisedSymbol);
      holder.accept(result.symbol());
    });
    return holder.get();
  }

  /**
   * Defines a new symbol and returns true if all when OK
   * But if there were errors created then false is returned.
   * This is expensive in the sense that it does the check and define by owning the re-entrant lock
   * on the compilable program.
   */
  public boolean defineOrError(final ISymbol symbol, final SymbolChecker symbolChecker) {
    AtomicBoolean rtn = new AtomicBoolean(true);

    //Must own the lock to be able to check for and define symbol
    compilableProgramContext.accept(compilableProgram -> {
      var errors = symbolChecker.errorsIfSymbolAlreadyDefined(this, symbol, false);
      if (!errors) {
        define(symbol);
      }
      rtn.set(!errors);
    });

    return rtn.get();
  }

  @Override
  public Optional<ISymbol> resolve(final SymbolSearch search) {
    var rtn = resolveInThisModuleOnly(search);
    return rtn.isPresent() ? rtn : resolveWithEnclosingScope(search);
  }

  /**
   * Looks to resolve search in either the references held or the symbols
   * in all the scopes for this module.
   */
  public Optional<ISymbol> resolveInThisModuleOnly(final SymbolSearch search) {
    Holder<ISymbol> rtn = new Holder<>();

    compilableProgramContext.accept(compilableProgram -> {
      //Try and resolve in this scope name from one of that scopes modules.
      rtn.accept(compilableProgram.resolveFromModule(getScopeName(), search));

      if (rtn.isEmpty() && !search.isLimitToBlocks()) {
        rtn.accept(compilableProgram.resolveReferenceFromModule(getScopeName(), search));
      }
    });
    return rtn.get();
  }

  /**
   * Just does a search in this particular module scope.
   * Which means as there can be multiple module scopes in the name module, this is only a partial search.
   */
  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(final SymbolSearch search) {
    AssertValue.checkNotNull("Search cannot be null", search);

    if (searchIsNotInThisScope(search)) {
      return Optional.empty();
    }

    String searchName = ISymbol.getUnqualifiedName(search.getName());
    var localScopeSearch = new SymbolSearch(ISymbol.makeFullyQualifiedName(getScopeName(), searchName)).setSearchType(
        search.getSearchType());

    localScopeSearch.setExampleSymbolToMatch(search.getExampleSymbolToMatch());

    Optional<ISymbol> resolvedSymbol = super.resolveInThisScopeOnly(localScopeSearch);

    if (resolvedSymbol.isEmpty()) {
      resolvedSymbol = resolveReferenceInThisScopeOnly(search);
    }

    return resolvedSymbol;
  }

  /**
   * Does a check if there is a references of that symbol already held in this module scope.
   * But remember there can be multiple of these per named module.
   */
  public Optional<ISymbol> resolveReferenceInThisScopeOnly(SymbolSearch search) {

    //Check by short name (i.e. unqualified)
    var unqualifiedName = ISymbol.getUnqualifiedName(search.getName());
    var resolvedSymbol = Optional.ofNullable(referencesScope.get(unqualifiedName));

    //If not the right category then not a match.
    //But a null in the search category means we are happy with just the name match.
    if (search.getSearchType() != null
        && resolvedSymbol.isPresent()
        && !resolvedSymbol.get().getCategory().equals(search.getSearchType())) {
      resolvedSymbol = Optional.empty();
    }
    return resolvedSymbol;
  }

  @Override
  protected Optional<ISymbol> resolveWithEnclosingScope(SymbolSearch search) {
    // Need to get result into a variable we can use outside of lambda
    // but we need the lambda to ensure access is thread safe.
    Holder<ISymbol> rtn = new Holder<>();

    compilableProgramContext.accept(compilableProgram -> {
      //If it is fully qualified let program scope workout module and resolve it.
      //But if it is this module we will have already search for it.
      var searchModule = ISymbol.getModuleNameIfPresent(search.getName());
      if (ISymbol.isQualifiedName(search.getName()) && !getScopeName().equals(searchModule)) {
        rtn.accept(compilableProgram.resolveByFullyQualifiedSearch(search));
      } else {

        //Only if we did not find anything and not limited to blocks do we search
        if (rtn.isEmpty() && !search.isLimitToBlocks()) {
          //Now these will be things like org.ek9.lang and or.ek9.math
          rtn.accept(compilableProgram.resolveFromImplicitScopes(search));
        }
      }
    });
    return rtn.get();
  }
}
