package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.files.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;

/**
 * SINGLE THREADED - Run across sources to check for duplicate definitions.
 * This could also include where we have referenced another module and specific
 * type/function and that now clashes with one in our own module.
 * For example, we define MySpecialType in our module 'com.specials', but also reference
 * 'net.stuff:MySpecialType' we can use both in shorthand.
 * Now in a multi source compile we need to check that for the same module - the same class names,
 * functions have not been redefined.
 */
public class Ek9Phase1SymbolDuplicationChecks implements
    BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final CompilationListener listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  public Ek9Phase1SymbolDuplicationChecks(CompilationListener listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.DUPLICATE_CHECKS;
    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }
}
