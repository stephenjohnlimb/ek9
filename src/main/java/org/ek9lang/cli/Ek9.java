package org.ek9lang.cli;

import java.util.concurrent.ExecutionException;
import org.ek9lang.LanguageMetaData;
import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.Logger;
import org.ek9lang.core.utils.OsSupport;
import org.ek9lang.lsp.Server;

/**
 * Main entry point into the compiler, build system and language server.
 * Will probably be wrapped in a native C executable at some point - or even moved native.
 * But for now just use java -jar ek9.jar
 * java -jar ek9.jar -C ./some/path/to/file.ek9
 * From an end user point of view they would interact with the script; so they would only write:
 * ./some/path/to/file.ek9 -d 2 -f someother.txt
 * or ek9 ./some/path/to/file.ek9 -d 2 -f someother.txt
 * etc
 * ./some/path/to/file.ek9 -r UDPServer1 -d 2 -f someother.txt
 * Note exit code 0 means there is a command run printed to stdout.
 * Exit code 1 means there is no further command to run but what you asked to do worked.
 * Exit code 2 means there was something wrong with the command line parameters.
 * Exit code 3 means there was some sort of file processing issue not found, missing content etc.
 * Exit code 4 means you used an invalid combination of command line parameters or there were
 * inappropriate.
 * Exit code 5 means the ek9 file you specified does not have any programs in it.
 * Exit code 6 means the ek9 file does have more than one program, and you did not specify
 * which to run.
 * Exit code 7 means the ek9 compiler when running as LSP failed.
 */
public class Ek9 {
  private static final LanguageMetaData languageMetaData = new LanguageMetaData("0.0.1-0");

  /**
   * The range of exit codes that EK9 will use.
   */

  public static final int RUN_COMMAND_EXIT_CODE = 0;
  public static final int SUCCESS_EXIT_CODE = 1;
  public static final int BAD_COMMANDLINE_EXIT_CODE = 2;
  public static final int FILE_ISSUE_EXIT_CODE = 3;
  public static final int BAD_COMMAND_COMBINATION_EXIT_CODE = 4;
  public static final int NO_PROGRAMS_EXIT_CODE = 5;
  public static final int PROGRAM_NOT_SPECIFIED_EXIT_CODE = 6;
  public static final int LANGUAGE_SERVER_NOT_STARTED_EXIT_CODE = 7;
  private final CommandLineDetails commandLine;

  public Ek9(CommandLineDetails commandLine) {
    this.commandLine = commandLine;
  }

  /**
   * Run the main Ek9 compiler.
   */
  public static void main(String[] argv) throws InterruptedException {
    OsSupport osSupport = new OsSupport();
    FileHandling fileHandling = new FileHandling(osSupport);
    CommandLineDetails commandLine =
        new CommandLineDetails(languageMetaData, fileHandling, osSupport);

    try {
      int result = commandLine.processCommandLine(argv);
      if (result >= Ek9.SUCCESS_EXIT_CODE) {
        System.exit(result);
      }

      System.exit(new Ek9(commandLine).run());
    } catch (RuntimeException rex) {
      Logger.error(rex);
      System.exit(BAD_COMMANDLINE_EXIT_CODE);
    }
  }

  /**
   * Run the command line and return the exit code.
   */
  public int run() throws InterruptedException {
    //This will cause the application to block and remain running as a language server.
    if (commandLine.isRunEk9AsLanguageServer()) {
      return runAsLanguageServer(commandLine);
    }

    //Just run the command as is.
    return runAsCommand(commandLine);
  }

  /**
   * Just run as a language server.
   * Blocks in the running.
   *
   * @param commandLine The commandline developer used.
   * @return The exit code to exit with
   */
  @SuppressWarnings("java:S106")
  private int runAsLanguageServer(CommandLineDetails commandLine) throws InterruptedException {
    Logger.error("EK9 running as LSP languageHelp=" + commandLine.isEk9LanguageServerHelpEnabled());
    var startListening =
        Server.runEk9LanguageServer(commandLine.getOsSupport(), System.in, System.out,
            commandLine.isEk9LanguageServerHelpEnabled());

    try {
      startListening.get();
    } catch (ExecutionException executionException) {
      Logger.error("Failed to Start Language Server");
      return LANGUAGE_SERVER_NOT_STARTED_EXIT_CODE;
    } catch (InterruptedException interruptedException) {
      Logger.error("Start Language Server Interrupted Stopping");
      throw interruptedException;
    }
    return SUCCESS_EXIT_CODE;
  }

  /**
   * Just run a normal EK9 commandline command.
   */
  private int runAsCommand(CommandLineDetails commandLine) {
    //Keep a cache once files are loaded, because each of the executions may call each other.
    FileCache sourceFileCache = new FileCache(commandLine);

    deleteFinalArtifactIfDependenciesAltered(sourceFileCache);

    int rtn = BAD_COMMANDLINE_EXIT_CODE;
    E execution = null;

    if (commandLine.isJustBuildTypeOption()) {
      execution = getExecutionForBuildTypeOption(sourceFileCache);
      rtn = SUCCESS_EXIT_CODE;
    } else if (commandLine.isReleaseVectorOption()) {
      execution = getExecutionForReleaseVectorOption(sourceFileCache);
      rtn = SUCCESS_EXIT_CODE;
    } else if (commandLine.isDeveloperManagementOption()) {
      execution = getExecutionForDeveloperManagementOption(sourceFileCache);
      rtn = SUCCESS_EXIT_CODE;
    } else if (commandLine.isUnitTestExecution()) {
      execution = new Et(commandLine, sourceFileCache);
      rtn = SUCCESS_EXIT_CODE;
    } else if (commandLine.isRunOption()) {
      execution = new Er(commandLine, sourceFileCache);
      rtn = RUN_COMMAND_EXIT_CODE;
    }

    if (execution == null || !execution.run()) {
      Logger.error("Command not executed");
      rtn = BAD_COMMANDLINE_EXIT_CODE;
    }
    return rtn;
  }

  private void deleteFinalArtifactIfDependenciesAltered(FileCache sourceFileCache) {
    if (commandLine.isDependenciesAltered()) {
      if (commandLine.isVerbose()) {
        Logger.error("Dependencies altered.");
      }
      sourceFileCache.deleteTargetExecutableArtefact();
    }
  }

  private E getExecutionForDeveloperManagementOption(FileCache sourceFileCache) {
    E execution = null;
    if (commandLine.isGenerateSigningKeys()) {
      execution = new Egk(commandLine, sourceFileCache);
    } else if (commandLine.isUpdateUpgrade()) {
      execution = new Up(commandLine, sourceFileCache);
    }
    return execution;
  }

  private E getExecutionForBuildTypeOption(FileCache sourceFileCache) {
    E execution = null;
    if (commandLine.isCleanAll()) {
      execution = new Ecl(commandLine, sourceFileCache);
    } else if (commandLine.isResolveDependencies()) {
      execution = new Edp(commandLine, sourceFileCache);
    } else if (commandLine.isIncrementalCompile()) {
      execution = new Eic(commandLine, sourceFileCache);
    } else if (commandLine.isFullCompile()) {
      execution = new Efc(commandLine, sourceFileCache);
    } else if (commandLine.isPackaging()) {
      execution = new Ep(commandLine, sourceFileCache);
    } else if (commandLine.isInstall()) {
      execution = new Ei(commandLine, sourceFileCache);
    } else if (commandLine.isDeployment()) {
      execution = new Ed(commandLine, sourceFileCache);
    }
    return execution;
  }

  private E getExecutionForReleaseVectorOption(FileCache sourceFileCache) {
    E execution = null;
    if (commandLine.isIncrementReleaseVector()) {
      execution = new Eiv(commandLine, sourceFileCache);
    } else if (commandLine.isSetReleaseVector()) {
      execution = new Esv(commandLine, sourceFileCache);
    } else if (commandLine.isSetFeatureVector()) {
      execution = new Esf(commandLine, sourceFileCache);
    } else if (commandLine.isPrintReleaseVector()) {
      execution = new Epv(commandLine, sourceFileCache);
    }
    return execution;
  }
}
