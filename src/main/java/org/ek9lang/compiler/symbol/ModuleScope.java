package org.ek9lang.compiler.symbol;

import java.util.HashMap;
import java.util.Map;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.symbol.support.SymbolTable;
import org.ek9lang.core.threads.SharedThreadContext;

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

}
