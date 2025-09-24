package org.ek9lang.compiler.phase11;

import java.util.function.Consumer;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.SharedThreadContext;

/**
 * SINGLE THREADED
 * Optimise the generated code.
 * See compilationContext.commandLine().targetArchitecture to determine what to prepare to optimise.
 */
public class CodeOptimisation extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.CODE_OPTIMISATION;

  public CodeOptimisation(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                          final FileHandling fileHandling, final Consumer<CompilationEvent> listener,
                          final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);
  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    return true;
  }
}
