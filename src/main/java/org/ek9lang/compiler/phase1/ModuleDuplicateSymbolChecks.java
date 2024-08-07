package org.ek9lang.compiler.phase1;

import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilableSourceHasErrors;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.SharedThreadContext;

/**
 * Goes through each module name and checks each of the parsedModules in that module name to check
 * there is only a single copy of that symbol in the whole module name space.
 * This is because we allow multithreaded loading of each source file.
 * This is an additional 'paranoid' check to ensure no module has the same symbol name in use at the top level.
 * Eventually it will be retired - but during development - it may enable early warning of threading issues.
 * Or other issues. but it's best to catch as early as possible.
 */
public final class ModuleDuplicateSymbolChecks extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.DUPLICATION_CHECK;
  private final CompilableSourceHasErrors sourceHasErrors = new CompilableSourceHasErrors();

  /**
   * Create a new duplicate checker for modules contained in the compilable program.
   */
  public ModuleDuplicateSymbolChecks(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                     final Consumer<CompilationEvent> listener,
                                     final CompilerReporter reporter) {
    super(thisPhase, compilableProgramAccess, listener, reporter);
  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    compilableProgramAccess.accept(this::checkForDuplicateSymbols);

    return !sourceHasErrors.test(workspace.getSources());
  }

  /**
   * THIS IS WHERE THE duplicates are checked for.
   */
  private void checkForDuplicateSymbols(final CompilableProgram program) {

    program.getParsedModuleNames()
        .parallelStream()
        .map(program::getParsedModules)
        .forEach(this::checkModules);

  }

  private void checkModules(final List<ParsedModule> parsedModules) {

    final HashSet<ISymbol> dupChecks = new HashSet<>();

    for (var parsedModule : parsedModules) {
      final var scope = parsedModule.getModuleScope();

      for (var symbol : scope.getSymbolsForThisScope()) {

        if (!dupChecks.add(symbol)) {
          throw new CompilerException("Duplicate Symbol: '" + symbol.getFriendlyName() + "' "
              + symbol.getSourceToken().getSourceName()
              + " Line " + symbol.getSourceToken().getLine());
        }

      }
      AssertValue.checkNotNull("ParsedModule must be present for source", parsedModule);
    }

  }
}