package org.ek9lang.compiler.phase7;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilableSourceErrorCheck;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilationPhaseResult;
import org.ek9lang.compiler.common.CompilerReporter;

/**
 * If all has gone well in previous phases, this just adds the IR to the main program(s).
 */
public class Ek9Phase7ProgramWithIR implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private static final CompilationPhase thisPhase = CompilationPhase.PROGRAM_IR_CONFIGURATION;
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  public Ek9Phase7ProgramWithIR(Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {

    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }
}
