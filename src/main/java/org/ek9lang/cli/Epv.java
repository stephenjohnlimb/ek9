package org.ek9lang.cli;

/**
 * Print the version number of the package.
 */
final class Epv extends Eve {
  Epv(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "$Version: ";
  }

  @Override
  protected boolean doRun() {
    report(compilationContext.commandLine().getVersion());
    return true;
  }
}
