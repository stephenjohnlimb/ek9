package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * SINGLE THREADED - Run across sources to check for duplicate definitions.
 * This could also include where we have referenced another module and specific
 * type/function and that now clashes with one in our own module.
 * For example, we define MySpecialType in our module 'com.specials', but also reference
 * 'net.stuff:MySpecialType' we can use both in shorthand.
 * Now in a multi source compile we need to check that for the same module - the same class names,
 * functions have not been redefined.
 */
public class Ek9Phase1SymbolDuplicationChecks implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final CompilationListener listener;
  private final CompilerReporter reporter;
  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  /**
   * Create a new instance for checking of duplicate symbols.
   */
  public Ek9Phase1SymbolDuplicationChecks(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                          CompilationListener listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
    this.compilableProgramAccess = compilableProgramAccess;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.DUPLICATE_CHECKS;
    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }
}
