package org.ek9lang.cli;

import java.io.File;
import org.ek9lang.cli.support.FileCache;
import org.ek9lang.cli.support.Reporter;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;

/**
 * Abstract base for the command line ek9 commands.
 */
public abstract class E extends Reporter {
  protected final CommandLineDetails commandLine;
  protected final FileCache sourceFileCache;

  private boolean debuggingInstrumentation = false;
  private boolean devBuild = false;
  private boolean checkCompilationOnly = false;

  protected E(CommandLineDetails commandLine, FileCache sourceFileCache) {
    super(commandLine.isVerbose());
    this.commandLine = commandLine;
    this.sourceFileCache = sourceFileCache;
  }

  /**
   * Provide the report/log message prefix.
   */
  protected abstract String messagePrefix();

  protected FileHandling getFileHandling() {
    return commandLine.getFileHandling();
  }

  protected OsSupport getOsSupport() {
    return commandLine.getOsSupport();
  }

  public boolean run() {
    return preConditionCheck() && doRun() && postConditionCheck();
  }

  /**
   * Do a precondition check to ensure run can execute.
   *
   * @return true if all Ok false if precondition not met.
   */
  public boolean preConditionCheck() {
    log("Prepare");
    //Ensure the .ek9 directory exists in users home directory.
    getFileHandling().validateHomeEk9Directory(commandLine.targetArchitecture);
    return true;
  }

  public boolean postConditionCheck() {
    log("Complete");
    return true;
  }

  /**
   * Actually run the execution.
   */
  protected abstract boolean doRun();

  public boolean isDebuggingInstrumentation() {
    return debuggingInstrumentation || commandLine.isDebuggingInstrumentation();
  }

  public void setDebuggingInstrumentation(boolean debuggingInstrumentation) {
    this.debuggingInstrumentation = debuggingInstrumentation;
  }

  public boolean isDevBuild() {
    return devBuild || commandLine.isDevBuild();
  }

  public void setDevBuild(boolean devBuild) {
    this.devBuild = devBuild;
    this.sourceFileCache.setDevBuild(devBuild);
  }

  public boolean isCheckCompilationOnly() {
    return checkCompilationOnly;
  }

  public void setCheckCompilationOnly(boolean checkCompilationOnly) {
    this.checkCompilationOnly = checkCompilationOnly;
  }

  protected String getDotEk9Directory() {
    return getFileHandling().getDotEk9Directory(commandLine.getSourceFileDirectory());
  }

  protected File getMainGeneratedOutputDirectory() {
    return getFileHandling().getMainGeneratedOutputDirectory(getDotEk9Directory(),
        commandLine.targetArchitecture);
  }

  protected File getMainFinalOutputDirectory() {
    return getFileHandling().getMainFinalOutputDirectory(getDotEk9Directory(),
        commandLine.targetArchitecture);
  }

  protected File getDevGeneratedOutputDirectory() {
    return getFileHandling().getDevGeneratedOutputDirectory(getDotEk9Directory(),
        commandLine.targetArchitecture);
  }

  protected File getDevFinalOutputDirectory() {
    return getFileHandling().getDevFinalOutputDirectory(getDotEk9Directory(),
        commandLine.targetArchitecture);
  }
}
