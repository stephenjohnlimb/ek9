package org.ek9lang.cli;

import org.ek9lang.cli.support.CompilationContext;

/**
 * Print the version number of the package.
 */
public class Epv extends Eve {
  public Epv(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "$Version: ";
  }

  protected boolean doRun() {
    report(compilationContext.commandLine().getVersion());
    return true;
  }
}
