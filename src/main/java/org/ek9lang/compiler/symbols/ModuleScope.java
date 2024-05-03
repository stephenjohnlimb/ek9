package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.SymbolChecker;
import org.ek9lang.compiler.support.TypeSubstitution;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.SharedThreadContext;

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

  @Serial
  private static final long serialVersionUID = 1L;

  private final SharedThreadContext<CompilableProgram> compilableProgram;

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
  private final Map<String, IToken> originalReferenceResolution = new TreeMap<>();

  /**
   * Create a new ModuleScope with a specific name and reference to the compilable program it is part of.
   */
  public ModuleScope(final String scopeName, final SharedThreadContext<CompilableProgram> program) {

    super(scopeName);
    AssertValue.checkNotNull("CompilableProgram cannot be null", program);
    this.compilableProgram = program;

  }

  /**
   * Create a clone of this ModuleScope.
   */
  public ModuleScope clone(final SharedThreadContext<CompilableProgram> newContext) {

    ModuleScope cloned = new ModuleScope(this.getScopeName(), newContext);
    cloneIntoSymbolTable(this, this);

    return cloned;
  }

  @Override
  public boolean equals(final Object o) {

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
  public void defineReference(final IToken token, final ISymbol symbol) {

    AssertValue.checkNotNull("Token cannot be null", token);
    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    final var shortName = ISymbol.getUnqualifiedName(symbol.getName());
    AssertValue.checkFalse("Duplicate reference bing added", referencesScope.containsKey(shortName));
    referencesScope.put(shortName, symbol);
    originalReferenceResolution.put(shortName, token);

  }

  /**
   * Returns the original location a reference was made (if present).
   */
  public Optional<IToken> getOriginalReferenceLocation(final SymbolSearch search) {

    final var shortName = ISymbol.getUnqualifiedName(search.getName());

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
  public Optional<ISymbol> resolveOrDefine(final PossibleGenericSymbol parameterisedSymbol,
                                           final ErrorListener errorListener) {

    final var holder = new AtomicReference<Optional<ISymbol>>(Optional.empty());
    compilableProgram.accept(program -> {
      final var shouldCompleteSubstitution = program.getCompilationData().phase() == CompilationPhase.FULL_RESOLUTION;

      if (!shouldCompleteSubstitution) {
        final var returnSymbol = program.resolveOrDefine(parameterisedSymbol);
        returnSymbol.symbol().ifPresent(symbol -> holder.set(Optional.of(symbol)));
      } else {
        final var typeSubstitution = new TypeSubstitution(program::resolveOrDefine, errorListener);
        final var populatedTypeWithMethods = typeSubstitution.apply(parameterisedSymbol);
        holder.set(Optional.of(populatedTypeWithMethods));
      }

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

    final var rtn = new AtomicBoolean(true);

    //Must own the lock to be able to check for and define symbol
    compilableProgram.accept(program -> {
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

    final var rtn = resolveInThisModuleOnly(search);

    return rtn.isPresent() ? rtn : resolveWithEnclosingScope(search);
  }

  /**
   * Looks to resolve search in either the references held or the symbols
   * in all the scopes for this module.
   */
  public Optional<ISymbol> resolveInThisModuleOnly(final SymbolSearch search) {

    final var rtn = new AtomicReference<Optional<ISymbol>>();

    compilableProgram.accept(program -> {
      //Try and resolve in this scope name from one of that scopes modules.
      rtn.set(program.resolveFromModule(getScopeName(), search));

      if (rtn.get().isEmpty() && !search.isLimitToBlocks()) {
        rtn.set(program.resolveReferenceFromModule(getScopeName(), search));
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

    final var searchName = ISymbol.getUnqualifiedName(search.getName());
    final var localScopeSearch = new SymbolSearch(ISymbol.makeFullyQualifiedName(getScopeName(), searchName), search);
    final var resolvedSymbol = super.resolveInThisScopeOnly(localScopeSearch);

    if (resolvedSymbol.isPresent()) {
      return resolvedSymbol;
    }

    return resolveReferenceInThisScopeOnly(search);
  }

  /**
   * Does a check if there is a references of that symbol already held in this module scope.
   * But remember there can be multiple of these per named module.
   */
  public Optional<ISymbol> resolveReferenceInThisScopeOnly(final SymbolSearch search) {

    //Check by short name (i.e. unqualified)
    final var unqualifiedName = ISymbol.getUnqualifiedName(search.getName());
    final var resolvedSymbol = Optional.ofNullable(referencesScope.get(unqualifiedName));

    //If not the right category then not a match.
    //But a null in the search category means we are happy with just the name match.
    if (resolvedSymbol.isPresent() && !search.isCategoryAcceptable(resolvedSymbol.get().getCategory())) {
      return Optional.empty();
    }

    return resolvedSymbol;
  }

  @Override
  public Optional<ISymbol> resolveWithEnclosingScope(final SymbolSearch search) {

    // Need to get result into a variable we can use outside of lambda
    // but, we need the lambda to ensure access is thread safe.
    final var rtn = new AtomicReference<Optional<ISymbol>>(Optional.empty());

    compilableProgram.accept(program -> {
      //If it is fully qualified let program scope workout module and resolve it.
      //But if it is this module we will have already search for it.
      final var searchModule = ISymbol.getModuleNameIfPresent(search.getName());
      if (ISymbol.isQualifiedName(search.getName()) && !getScopeName().equals(searchModule)) {
        rtn.set(program.resolveByFullyQualifiedSearch(search));
      } else {

        //Only if we did not find anything and not limited to blocks do we search
        if (rtn.get().isEmpty() && !search.isLimitToBlocks()) {
          //Now these will be things like org.ek9.lang and or.ek9.math
          rtn.set(program.resolveFromImplicitScopes(search));
        }
      }
    });

    return rtn.get();
  }
}
