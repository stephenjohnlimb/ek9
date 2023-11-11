package org.ek9lang.compiler.phase4;

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
 * Once full resolution has completed, additional checks need to be made.
 * These relate to Generic Types, are assumed operators present on the type arguments.
 * Are the types used when subtyping constrained generic types appropriate.
 * But there could be several other post resolution checks if required.
 * Ideally most checks will have been done as early as possible, but as EK9 is quite
 * dynamic and has inference it means that not all checks can be completed until now.
 */
public class PostSymbolResolutionChecks extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.POST_RESOLUTION_CHECKS;

  /**
   * Create new instance to check everything is logical and cohesive.
   */
  public PostSymbolResolutionChecks(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                    Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    super(thisPhase, compilableProgramAccess, listener, reporter);
  }

  @Override
  public boolean doApply(Workspace workspace, CompilerFlags compilerFlags) {

    return true;
  }
}
