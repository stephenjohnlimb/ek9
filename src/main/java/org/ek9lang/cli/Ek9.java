package org.ek9lang.cli;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.ek9lang.LanguageMetaData;
import org.ek9lang.cli.support.CompilationContext;
import org.ek9lang.cli.support.FileCache;
import org.ek9lang.cli.support.Reporter;
import org.ek9lang.compiler.files.CompilableSource;
import org.ek9lang.compiler.main.Compiler;
import org.ek9lang.compiler.main.Ek9Compiler;
import org.ek9lang.compiler.main.phases.CompilationPhase;
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

  private final CompilationContext compilationContext;
  private final CompilationReporter reporter;

  /**
   * Creates the compilerContext with the commandLine, and a real compiler.
   */
  private static final Function<CommandLineDetails, CompilationContext> compilationContextCreation =
      commandLine -> {
        CompilationReporter compilerReporter = new CompilationReporter(commandLine.isVerbose());
        FileCache sourceFileCache = new FileCache(commandLine);
        Compiler compiler = new Ek9Compiler(compilerReporter::logPhaseCompilation);

        return new CompilationContext(commandLine, compiler, sourceFileCache);
      };

  public Ek9(CompilationContext compilationContext) {
    this.compilationContext = compilationContext;
    this.reporter = new CompilationReporter(compilationContext.commandLine().isVerbose());
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

      System.exit(new Ek9(compilationContextCreation.apply(commandLine)).run());
    } catch (RuntimeException rex) {
      Logger.error(rex);
      System.exit(BAD_COMMANDLINE_EXIT_CODE);
    }
  }


  /**
   * Run the command line and return the exit code.
   * This can be either the language server or just the command line compiler.
   */
  public int run() throws InterruptedException {
    //This will cause the application to block and remain running as a language server.
    if (compilationContext.commandLine().isRunEk9AsLanguageServer()) {
      return runAsLanguageServer();
    }

    //Just run the command as is.
    return runAsCommand();
  }

  /**
   * Just run as a language server.
   * Blocks in the running.
   *
   * @return The exit code to exit with
   */
  @SuppressWarnings("java:S106")
  private int runAsLanguageServer() throws InterruptedException {
    reporter.report("EK9 running as LSP languageHelp=" + compilationContext.commandLine()
        .isEk9LanguageServerHelpEnabled());
    var startListening =
        Server.runEk9LanguageServer(compilationContext.commandLine().getOsSupport(), System.in,
            System.out,
            compilationContext.commandLine().isEk9LanguageServerHelpEnabled());

    try {
      startListening.get();
    } catch (ExecutionException executionException) {
      reporter.report("Failed to Start Language Server");
      return LANGUAGE_SERVER_NOT_STARTED_EXIT_CODE;
    } catch (InterruptedException interruptedException) {
      reporter.report("Start Language Server Interrupted Stopping");
      throw interruptedException;
    }
    return SUCCESS_EXIT_CODE;
  }

  /**
   * Just run a normal EK9 commandline command - which could call the compiler.
   */
  private int runAsCommand() {

    deleteFinalArtifactIfDependenciesAltered(compilationContext);

    int rtn = BAD_COMMANDLINE_EXIT_CODE;
    E execution = null;

    if (compilationContext.commandLine().isJustBuildTypeOption()) {
      execution = getExecutionForBuildTypeOption(compilationContext);
      rtn = SUCCESS_EXIT_CODE;
    } else if (compilationContext.commandLine().isReleaseVectorOption()) {
      execution = getExecutionForReleaseVectorOption(compilationContext);
      rtn = SUCCESS_EXIT_CODE;
    } else if (compilationContext.commandLine().isDeveloperManagementOption()) {
      execution = getExecutionForDeveloperManagementOption(compilationContext);
      rtn = SUCCESS_EXIT_CODE;
    } else if (compilationContext.commandLine().isUnitTestExecution()) {
      execution = new Et(compilationContext);
      rtn = SUCCESS_EXIT_CODE;
    } else if (compilationContext.commandLine().isRunOption()) {
      execution = new Er(compilationContext);
      rtn = RUN_COMMAND_EXIT_CODE;
    }

    //If the command line was to call for compilation this execution will trigger it.
    if (execution == null || !execution.run()) {
      reporter.report("Command not executed");
      rtn = BAD_COMMANDLINE_EXIT_CODE;
    }
    return rtn;
  }

  private void deleteFinalArtifactIfDependenciesAltered(CompilationContext compilationContext) {
    if (compilationContext.commandLine().isDependenciesAltered()) {
      reporter.log("Dependencies altered.");
      compilationContext.sourceFileCache().deleteTargetExecutableArtefact();
    }
  }

  private E getExecutionForDeveloperManagementOption(CompilationContext compilationContext) {
    E execution = null;
    if (compilationContext.commandLine().isGenerateSigningKeys()) {
      execution = new Egk(compilationContext);
    } else if (compilationContext.commandLine().isUpdateUpgrade()) {
      execution = new Up(compilationContext);
    }
    return execution;
  }

  private E getExecutionForBuildTypeOption(CompilationContext compilationContext) {
    E execution = null;
    if (compilationContext.commandLine().isCleanAll()) {
      execution = new Ecl(compilationContext);
    } else if (compilationContext.commandLine().isResolveDependencies()) {
      execution = new Edp(compilationContext);
    } else if (compilationContext.commandLine().isIncrementalCompile()) {
      execution = new Eic(compilationContext);
    } else if (compilationContext.commandLine().isFullCompile()) {
      execution = new Efc(compilationContext);
    } else if (compilationContext.commandLine().isPackaging()) {
      execution = new Ep(compilationContext);
    } else if (compilationContext.commandLine().isInstall()) {
      execution = new Ei(compilationContext);
    } else if (compilationContext.commandLine().isDeployment()) {
      execution = new Ed(compilationContext);
    }
    return execution;
  }

  private E getExecutionForReleaseVectorOption(CompilationContext compilationContext) {
    E execution = null;
    if (compilationContext.commandLine().isIncrementReleaseVector()) {
      execution = new Eiv(compilationContext);
    } else if (compilationContext.commandLine().isSetReleaseVector()) {
      execution = new Esv(compilationContext);
    } else if (compilationContext.commandLine().isSetFeatureVector()) {
      execution = new Esf(compilationContext);
    } else if (compilationContext.commandLine().isPrintReleaseVector()) {
      execution = new Epv(compilationContext);
    }
    return execution;
  }

  /**
   * Just used for reporting and logging errors and warnings.
   */
  private static class CompilationReporter extends Reporter {
    protected CompilationReporter(boolean verbose) {
      super(verbose);
    }

    public void logPhaseCompilation(CompilationPhase phase, CompilableSource source) {
      var errorListener = source.getErrorListener();
      log(String.format("%s: %s processed", phase, source.getFileName()));

      if (errorListener.hasWarnings()) {
        report(String.format("%s: %s has warnings", phase, source.getFileName()));
        errorListener.getWarnings().forEachRemaining(this::report);
      }

      if (errorListener.hasErrors()) {
        report(String.format("%s: Errors in %s", phase, source.getFileName()));
        errorListener.getErrors().forEachRemaining(this::report);
      }
    }

    @Override
    protected String messagePrefix() {
      return "EK9     : ";
    }
  }
}
