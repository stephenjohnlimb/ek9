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
public class IRGeneration extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.SIMPLE_IR_GENERATION;

  public IRGeneration(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                      final Consumer<CompilationEvent> listener,
                      final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    return true;
  }
}
