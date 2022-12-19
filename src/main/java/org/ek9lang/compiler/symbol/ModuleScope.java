package org.ek9lang.compiler.symbol;

import java.util.Optional;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.symbol.support.SymbolTable;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.threads.SharedThreadContext;
import org.ek9lang.core.utils.Holder;

/**
 * This is a very special scope, because the same module can be defined in
 * multiple files. So when looking to resolve variables we need to look up the
 * scope tree back to the global scope - but when we get here to this module
 * scope we need to look across all the other modules scopes (same module name)
 * to check in those as well.
 * Now we may also need to consider looking in a range of other named modules if those other modules
 * have been listed in a 'references' statement - but this is yet to be done!
 */
public class ModuleScope extends SymbolTable {

  private final SharedThreadContext<CompilableProgram> compilableProgramContext;

  public ModuleScope(String scopeName, SharedThreadContext<CompilableProgram> context) {
    super(scopeName);
    this.compilableProgramContext = context;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    var rtn = false;
    if (obj instanceof ModuleScope moduleScope) {
      rtn = getFriendlyScopeName().equals(moduleScope.getFriendlyScopeName());
    }
    return rtn;
  }


  /**
   * Looks to resolve search in either the references held or the symbols in this scope.
   */
  public Optional<ISymbol> resolveInThisModuleOnly(SymbolSearch search) {
    Optional<ISymbol> resolvedSymbol = resolveReferenceInThisModuleOnly(search);

    if (!resolvedSymbol.isPresent()) {
      resolvedSymbol = resolveInThisScopeOnly(search);
    }

    return resolvedSymbol;
  }

  /**
   * Does a check if there is a references of that symbol already held in this module scope.
   * But remember there can be multiple of these per named module.
   */
  public Optional<ISymbol> resolveReferenceInThisModuleOnly(SymbolSearch search) {
    //TODO implement
    return Optional.empty();
  }

  @Override
  protected Optional<ISymbol> resolveWithEnclosingScope(SymbolSearch search) {
    // Need to get result into a variable we can use outside of lambda
    // but we need the lambda to ensure access is thread safe.
    Holder<ISymbol> rtn = new Holder<>();

    compilableProgramContext.accept(compilableProgram -> {
      // see if we can find in global context from modules with same scope
      // name as this.
      // This is because multiple files can have same module name.
      rtn.accept(compilableProgram.resolveFromModule(getScopeName(), search));

      //Only if we did not find anything and not limited to blocks do we search
      if (rtn.isEmpty() && !search.isLimitToBlocks()) {
        //Now these will be things like org.ek9.lang and or.ek9.math
        rtn.accept(compilableProgram.resolveFromImplicitScopes(search));
      }
    });

    return rtn.get();
  }
}
