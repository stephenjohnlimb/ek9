package org.ek9lang.cli;

/**
 * Used to explicitly set the version of a package.
 */
final class Esv extends Eve {
  Esv(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Version=: ";
  }

  @Override
  protected boolean doRun() {
    String newVersionParameter = compilationContext.commandLine().getOptionParameter("-SV");
    return super.setVersionNewNumber(Version.withNoBuildNumber(newVersionParameter));
  }
}
