package org.ek9lang.cli;

/**
 * Used to explicitly set the version of a package.
 */
public class Esv extends Eve {
  public Esv(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Version=: ";
  }

  protected boolean doRun() {
    String newVersionParameter = compilationContext.commandLine().getOptionParameter("-SV");
    return super.setVersionNewNumber(Version.withNoBuildNumber(newVersionParameter));
  }
}
