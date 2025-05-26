package org.ek9lang.compiler.phase6;

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
 * No-op at this stage. Ready for if we ever do need to plugin anything.
 * The idea will be to enable a developer to include a module that uses the word 'external'
 * and in effect just provides type/method signatures in EK9, but then at this phase the compiler
 * attempt to locate the actual 'shared library' or 'jar' file to ensure for the target architecture
 * compilation and linking can actually be achieved.
 */
public class PluginResolution extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.PLUGIN_RESOLUTION;

  /**
   * Create new instance to resolve plugins for extern packages.
   */
  public PluginResolution(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                          final Consumer<CompilationEvent> listener,
                          final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    return true;
  }
}
