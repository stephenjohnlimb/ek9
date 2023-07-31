package org.ek9lang.cli;

/**
 * Use to explicitly set a feature name on a package.
 */
final class Esf extends Eve {
  Esf(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Feature=: ";
  }

  @Override
  protected boolean doRun() {
    String newVersionParameter = compilationContext.commandLine().getOptionParameter("-SF");
    return super.setVersionNewNumber(Version.withNoBuildNumber(newVersionParameter));
  }
}