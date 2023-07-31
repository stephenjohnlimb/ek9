package org.ek9lang.compiler.phase10;

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
 * MULTI THREADED
 * Generate all functions (including dynamic and templated) that have been defined.
 * See compilationContext.commandLine().targetArchitecture to determine what to prepare to create.
 */
public class CodeGenerationFunctions implements
    BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private static final CompilationPhase thisPhase = CompilationPhase.CODE_GENERATION_FUNCTIONS;
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  public CodeGenerationFunctions(Consumer<CompilationEvent> listener,
                                 CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {

    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }
}
