package org.ek9lang.cli;

import java.io.File;
import org.ek9lang.compiler.common.Reporter;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.OsSupport;

/**
 * Abstract base for the command line ek9 commands.
 */
abstract class E extends Reporter {
  protected final CompilationContext compilationContext;

  E(CompilationContext compilationContext) {
    super(compilationContext.commandLine().isVerbose(), compilationContext.muteReportedErrors());
    this.compilationContext = compilationContext;
  }

  boolean run() {
    return preConditionCheck() && doRun() && postConditionCheck();
  }

  /**
   * Do a precondition check to ensure run can execute.
   *
   * @return true if all Ok false if precondition not met.
   */
  boolean preConditionCheck() {
    log("Prepare");
    //Ensure the .ek9 directory exists in users home directory.
    getFileHandling().validateHomeEk9Directory(compilationContext.commandLine().targetArchitecture);
    return true;
  }

  boolean postConditionCheck() {
    log("Complete");
    return true;
  }

  @Override
  protected abstract String messagePrefix();

  protected FileHandling getFileHandling() {
    return compilationContext.commandLine().getFileHandling();
  }

  protected OsSupport getOsSupport() {
    return compilationContext.commandLine().getOsSupport();
  }

  /**
   * Actually run the execution.
   */
  protected abstract boolean doRun();

  protected String getDotEk9Directory() {
    return getFileHandling().getDotEk9Directory(
        compilationContext.commandLine().getSourceFileDirectory());
  }

  protected File getMainGeneratedOutputDirectory() {
    return getFileHandling().getMainGeneratedOutputDirectory(getDotEk9Directory(),
        compilationContext.commandLine().targetArchitecture);
  }

  protected File getMainFinalOutputDirectory() {
    return getFileHandling().getMainFinalOutputDirectory(getDotEk9Directory(),
        compilationContext.commandLine().targetArchitecture);
  }

  protected File getDevGeneratedOutputDirectory() {
    return getFileHandling().getDevGeneratedOutputDirectory(getDotEk9Directory(),
        compilationContext.commandLine().targetArchitecture);
  }

  protected File getDevFinalOutputDirectory() {
    return getFileHandling().getDevFinalOutputDirectory(getDotEk9Directory(),
        compilationContext.commandLine().targetArchitecture);
  }
}
