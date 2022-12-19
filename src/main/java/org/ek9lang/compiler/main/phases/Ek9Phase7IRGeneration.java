package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;

/**
 * MULTI THREADED
 * All symbols resolve and so should be able to create an intermediate representation.
 * But this phase will be for non-generic types ONLY - see later how the IR is created for
 * Template/generic types.
 * NOTE: when processing and generating nodes - YOU must visit down the tree - don't be tempted
 * to use just the symbols from the previous phases. That was mainly to ensure semantics,
 * this is 'the generate an IR' yes you can use information from the previous stages but the
 * types of nodes generated are really important and it is the context of where they are defined
 * that adds real value.
 */
public class Ek9Phase7IRGeneration implements
    BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final CompilationPhaseListener listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  public Ek9Phase7IRGeneration(CompilationPhaseListener listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.SIMPLE_IR_GENERATION;
    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }
}
