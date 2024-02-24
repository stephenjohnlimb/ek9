package org.ek9lang.compiler.common;

/**
 * Just expends the standard reporter to use the EK9Comp message prefix.
 * This is so when compiler errors and warnings are issued, they can be seen to
 * be from the compiler itself.
 */
public class CompilerReporter extends Reporter {
  public CompilerReporter(final boolean verbose, final boolean muteReportedErrors) {
    super(verbose, muteReportedErrors);
  }

  @Override
  protected String messagePrefix() {
    return "EK9Comp : ";
  }
}
