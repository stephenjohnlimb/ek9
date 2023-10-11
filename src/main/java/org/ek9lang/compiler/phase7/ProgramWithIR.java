package org.ek9lang.compiler.phase7;

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
 * If all has gone well in previous phases, this just adds the IR to the main program(s).
 */
public class ProgramWithIR extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.PROGRAM_IR_CONFIGURATION;

  public ProgramWithIR(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                       Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    super(thisPhase, compilableProgramAccess, listener, reporter);
  }

  @Override
  public boolean doApply(Workspace workspace, CompilerFlags compilerFlags) {
    return true;
  }
}
