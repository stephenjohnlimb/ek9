package org.ek9lang.compiler.phase9;

import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.core.SharedThreadContext;

/**
 * SINGLE THREADED.
 * At this point IR is complete and viable. This phase can now optimise the IR prior to any
 * code generation.
 */
public class IROptimisation extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.IR_OPTIMISATION;

  public IROptimisation(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                        final Consumer<CompilationEvent> listener,
                        final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    return true;
  }
}
