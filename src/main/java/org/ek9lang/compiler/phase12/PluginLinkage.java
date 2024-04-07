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
 * If the developer has employed any sort of 'native' plugin, then it will have been referenced
 * and trusted at earlier phases. PluginResolution will have done some checks. But here we must now
 * actually check and 'link' what has been referenced to the actual code.
 */
public class PluginLinkage extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.PLUGIN_LINKAGE;

  public PluginLinkage(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                       final Consumer<CompilationEvent> listener,
                       final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);

  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    return true;
  }
}
