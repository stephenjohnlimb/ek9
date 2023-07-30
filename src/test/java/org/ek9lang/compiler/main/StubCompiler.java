package org.ek9lang.compiler.main;

import org.ek9lang.compiler.Workspace;

/**
 * Just a dummy to be used during testing of components.
 */
public class StubCompiler implements Compiler {

  @Override
  public boolean compile(Workspace workspace, CompilerFlags flags) {
    return true;
  }
}
