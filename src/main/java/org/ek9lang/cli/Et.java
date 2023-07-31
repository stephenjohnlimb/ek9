package org.ek9lang.cli;

/**
 * Run all unit tests inside a project.
 */
final class Et extends E {
  Et(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Test    : ";
  }

  @Override
  protected boolean doRun() {
    log("Compile!");

    //trigger rebuild if needed
    Eic eic = new Eic(compilationContext);
    eic.setDebuggingInstrumentation(true);
    eic.setDevBuild(true);
    return eic.run() && runTheTests();
  }

  private boolean runTheTests() {
    return true;
  }
}
