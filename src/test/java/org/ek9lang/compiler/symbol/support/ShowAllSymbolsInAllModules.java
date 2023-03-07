package org.ek9lang.compiler.symbol.support;

import java.util.function.Consumer;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * Accesses all the modules and dumps out all the Symbols in each.
 */
public class ShowAllSymbolsInAllModules implements Consumer<SharedThreadContext<CompilableProgram>> {

  private final ShowSymbolInModule showSymbolInModule = new ShowSymbolInModule();

  @Override
  public void accept(SharedThreadContext<CompilableProgram> compilableProgramSharedThreadContext) {
    compilableProgramSharedThreadContext.accept(program ->
        program.getParsedModuleNames().stream().map(program::getParsedModules).forEach(showSymbolInModule));
  }
}
