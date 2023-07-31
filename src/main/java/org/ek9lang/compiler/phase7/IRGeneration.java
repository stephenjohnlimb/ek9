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
public class IRGeneration implements
    BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private static final CompilationPhase thisPhase = CompilationPhase.SIMPLE_IR_GENERATION;
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  public IRGeneration(Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {

    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }
}
