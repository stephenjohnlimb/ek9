package org.ek9lang.compiler.phase10;

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
 * Generate all functions (including dynamic and templated) that have been defined.
 * See compilationContext.commandLine().targetArchitecture to determine what to prepare to create.
 */
public class CodeGenerationFunctions extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.CODE_GENERATION_FUNCTIONS;

  public CodeGenerationFunctions(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                 final Consumer<CompilationEvent> listener,
                                 final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    return true;
  }
}
