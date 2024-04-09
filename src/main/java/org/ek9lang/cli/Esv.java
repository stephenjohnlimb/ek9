package org.ek9lang.cli;

/**
 * Used to explicitly set the version of a package.
 */
final class Esv extends Eve {
  Esv(final CompilationContext compilationContext) {

    super(compilationContext);

  }

  @Override
  protected String messagePrefix() {

    return "Version=: ";
  }

  @Override
  protected boolean doRun() {

    final var newVersionParameter = compilationContext.commandLine().getOptionParameter("-SV");

    return super.setVersionNewNumber(Version.withNoBuildNumber(newVersionParameter));
  }
}
