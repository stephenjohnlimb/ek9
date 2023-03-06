package org.ek9lang.compiler.main.directives;

import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.errors.CompilationPhaseListener;

/**
 * Just checks if there are any directives that relate to @Resolved in the parsed module and checks the
 * non-resolution.
 */
public class NotResolvedDirectiveListener implements CompilationPhaseListener {
  @Override
  public void accept(CompilationEvent compilationEvent) {

  }
}
