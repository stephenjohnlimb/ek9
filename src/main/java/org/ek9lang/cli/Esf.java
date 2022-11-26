package org.ek9lang.cli;

import org.ek9lang.cli.support.CompilationContext;

/**
 * Use to explicitly set a feature name on a package.
 */
public class Esf extends Eve {
  public Esf(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Feature=: ";
  }

  protected boolean doRun() {
    String newVersionParameter = compilationContext.commandLine().getOptionParameter("-SF");
    return super.setVersionNewNumber(Version.withNoBuildNumber(newVersionParameter));
  }
}