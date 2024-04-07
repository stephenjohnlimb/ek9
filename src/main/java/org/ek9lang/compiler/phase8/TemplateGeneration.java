package org.ek9lang.compiler.phase8;

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
 * Now just create all the nodes that are generated from generic types
 * So these will be the set of concrete template classes we are using throughout the program all
 * modules!
 * TODO need to think about where the concrete templates should be recorded.
 * They will be recorded in the same module as their generic type - clearly we will need to
 * add functionality to detect this and generate, as the generic type might be an 'extern'.
 * The generic base they are created from has to be revisited which means we need the right
 * parsed module.
 */
public class TemplateGeneration extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.TEMPLATE_IR_GENERATION;

  public TemplateGeneration(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                            final Consumer<CompilationEvent> listener,
                            final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    return true;
  }

}
