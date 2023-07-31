package org.ek9lang.cli;

/**
 * Update/Upgrade the Ek9 compiler itself.
 */
final class Up extends E {
  Up(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Update  : ";
  }

  @Override
  protected boolean doRun() {
    log("Would check compiler version and available version and update.");
    return true;
  }
}
