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
 * At this point all sources have parsed, all symbols resolved and IR is built.
 * During the creation of the IR some analysis will have already taken place;
 * this is aimed at 'failing early'. This enables the developer to address issues in
 * large code bases much earlier.
 * However, there is some analysis that can only take place once we have the whole IR in place.
 * That's what this phase is for, analysing the whole IR and doing any and all final checks before
 * optimisation takes place.
 */
public class IRAnalysis extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.IR_ANALYSIS;

  public IRAnalysis(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                    final Consumer<CompilationEvent> listener,
                    final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    return true;
  }
}
