package org.ek9lang.cli;

import org.ek9lang.compiler.Compiler;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.Workspace;

/**
 * Just a dummy to be used during testing of components.
 */
final class StubCompiler implements Compiler {

  @Override
  public boolean compile(Workspace workspace, CompilerFlags flags) {
    return true;
  }
}
