package org.ek9lang.compiler.phase12;

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
 * SINGLE THREADED
 * Complete any post-processing required. This could involve converting applications to
 * binary executable for example.
 */
public class PackagingPostProcessing extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.PACKAGING_POST_PROCESSING;

  public PackagingPostProcessing(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                 final Consumer<CompilationEvent> listener,
                                 final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    return true;
  }
}
