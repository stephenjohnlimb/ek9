package org.ek9lang.compiler.main.phases;

import java.util.HashSet;
import java.util.function.BiFunction;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.exception.CompilerException;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * Goes through each module name and checks each of the parsedModules in that module name to check
 * there is only a single copy of that symbol in the whole module name space.
 * This is because we allow multithreaded loading of each source file.
 * This is an additional 'paranoid' check to ensure no module has the same symbol name in use at the top level.
 * Eventually it will be retired - but during development - it may enable early warning of threading issues.
 * Or other issues. but it's best to catch as early as possible.
 */
public class Ek9Phase1ModuleDuplicateSymbolChecks
    implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final CompilationPhaseListener listener;
  private final CompilerReporter reporter;
  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  /**
   * Create a new duplicate checker for modules contained in the compilable program.
   */
  public Ek9Phase1ModuleDuplicateSymbolChecks(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                              CompilationPhaseListener listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
    this.compilableProgramAccess = compilableProgramAccess;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.DUPLICATION_CHECK;
    reporter.log(thisPhase);

    //This will check and add any errors to the appropriate module source error listener.
    checkForDuplicateSymbols();

    var errorFree = !sourceHaveErrors.test(workspace.getSources());

    return new CompilationPhaseResult(thisPhase, errorFree,
        compilerFlags.getCompileToPhase() == thisPhase);
  }

  /**
   * THIS IS WHERE THE duplicates are checked for.
   */
  private void checkForDuplicateSymbols() {
    compilableProgramAccess.accept(program -> {

      for (var moduleName : program.getParsedModuleNames()) {
        var parsedModules = program.getParsedModules(moduleName);
        HashSet<String> dupChecks = new HashSet<>();
        for (var parsedModule : parsedModules) {
          var scope = parsedModule.getModuleScope();
          for (var symbol : scope.getSymbolsForThisScope()) {
            if (!dupChecks.add(symbol.getName())) {
              throw new CompilerException("How has a duplicate Symbol gor passed the reentrant lock "
                  + symbol.getSourceToken().getTokenSource().getSourceName()
                  + "Line " + symbol.getSourceToken().getLine());
            }
          }
          AssertValue.checkNotNull("ParsedModule must be present for source", parsedModule);
        }
      }

    });
  }
}