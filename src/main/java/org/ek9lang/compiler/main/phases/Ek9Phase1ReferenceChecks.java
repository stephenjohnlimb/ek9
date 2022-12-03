package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.files.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;

/**
 * SINGLE THREADED - because we only want one reference shorthand for an item.
 * We don't want two different files reference com.abc.Item and another file
 * com.def.Item in the same module - even though different source files.
 */
public class Ek9Phase1ReferenceChecks implements
    BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>  {
  private final CompilationListener listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  public Ek9Phase1ReferenceChecks(CompilationListener listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.REFERENCE_CHECKS;
    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }
}
