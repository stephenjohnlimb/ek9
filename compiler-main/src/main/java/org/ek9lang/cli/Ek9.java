package org.ek9lang.cli;

import static org.ek9lang.compiler.common.Ek9ExitCodes.BAD_COMMANDLINE_EXIT_CODE;
import static org.ek9lang.compiler.common.Ek9ExitCodes.COMPILATION_FAILED_EXIT_CODE;
import static org.ek9lang.compiler.common.Ek9ExitCodes.LANGUAGE_SERVER_NOT_STARTED_EXIT_CODE;
import static org.ek9lang.compiler.common.Ek9ExitCodes.RUN_COMMAND_EXIT_CODE;
import static org.ek9lang.compiler.common.Ek9ExitCodes.SUCCESS_EXIT_CODE;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.ek9lang.compiler.Ek9BuiltinIntrospectionSupplier;
import org.ek9lang.compiler.Ek9Compiler;
import org.ek9lang.compiler.Ek9LanguageBootStrap;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.common.Reporter;
import org.ek9lang.compiler.common.VerboseErrorMessages;
import org.ek9lang.compiler.config.FullPhaseSupplier;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.Logger;
import org.ek9lang.core.OsSupport;
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
 * Exit codes are defined in {@link org.ek9lang.compiler.common.Ek9ExitCodes}.
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
 * Exit code 8 means the ek9 compilation failed with errors.
 * Exit code 9 means wrong number of arguments provided to program.
 * Exit code 10 means cannot convert argument to required type.
 */
final class Ek9 {
  private static final LanguageMetaData languageMetaData = new LanguageMetaData("0.0.1-0");

  /**
   * Creates the compilerContext with the commandLine, and a real compiler.
   */
  private static final Function<CommandLine, CompilationContext> compilationContextCreation =
      commandLine -> {
        // Enable verbose error messages if -ve flag specified (for AI-assisted development)
        VerboseErrorMessages.setVerboseEnabled(commandLine.options().isErrorVerbose());

        final var fileHandling = commandLine.getFileHandling();
        final var muteReportedErrors = false;
        final var compilationReporter = new CompilationReporter(commandLine.options().isVerbose());
        final var compilerReporter = new CompilerReporter(commandLine.options().isVerbose(), false);
        final var sourceFileCache = new FileCache(commandLine);
        final var sourceSupplier = new Ek9BuiltinIntrospectionSupplier();
        final var bootStrap =
            new Ek9LanguageBootStrap(sourceSupplier, compilationReporter::logPhaseCompilation, compilerReporter);
        final var allPhases = new FullPhaseSupplier(bootStrap.get(), fileHandling,
            compilationReporter::logPhaseCompilation, new CompilerReporter(commandLine.options().isVerbose(), false));
        final var compiler = new Ek9Compiler(allPhases, muteReportedErrors);

        return new CompilationContext(commandLine, compiler, sourceFileCache, muteReportedErrors,
            new CompilationResult());
      };

  private final CompilationContext compilationContext;
  private final CompilationReporter reporter;

  Ek9(final CompilationContext compilationContext) {

    this.compilationContext = compilationContext;
    this.reporter = new CompilationReporter(compilationContext.commandLine().options().isVerbose());

  }

  /**
   * Run the main Ek9 compiler.
   */
  public static void main(final String[] argv) throws InterruptedException {

    final var osSupport = new OsSupport();
    final var fileHandling = new FileHandling(osSupport);
    final var commandLine = new CommandLine(languageMetaData, fileHandling, osSupport);

    try {
      final var result = commandLine.process(argv);
      if (result >= SUCCESS_EXIT_CODE) {
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
  int run() throws InterruptedException {

    //This will cause the application to block and remain running as a language server.
    if (compilationContext.commandLine().options().isRunEk9AsLanguageServer()) {
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

    final var enabled = compilationContext.commandLine().options().isEk9LanguageServerHelpEnabled();
    reporter.report("EK9 running as LSP languageHelp=" + enabled);
    final var enableDebugOutput = compilationContext.commandLine().options().isDebugVerbose();
    final var startListening = Server.runEk9LanguageServer(compilationContext.commandLine().getOsSupport(), System.in,
        System.out, compilationContext.commandLine().options().isEk9LanguageServerHelpEnabled(), enableDebugOutput);

    try {
      startListening.get();
    } catch (ExecutionException executionException) {
      reporter.report("Failed to Start Language Server + " + executionException.getMessage());
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

    if (compilationContext.commandLine().options().isJustBuildTypeOption()) {
      execution = getExecutionForBuildTypeOption(compilationContext);
      rtn = SUCCESS_EXIT_CODE;
    } else if (compilationContext.commandLine().options().isReleaseVectorOption()) {
      execution = getExecutionForReleaseVectorOption(compilationContext);
      rtn = SUCCESS_EXIT_CODE;
    } else if (compilationContext.commandLine().options().isDeveloperManagementOption()) {
      execution = getExecutionForDeveloperManagementOption(compilationContext);
      rtn = SUCCESS_EXIT_CODE;
    } else if (compilationContext.commandLine().options().isUnitTestExecution()) {
      execution = new Et(compilationContext);
      rtn = SUCCESS_EXIT_CODE;
    } else if (compilationContext.commandLine().options().isRunOption()) {
      execution = new Er(compilationContext);
      rtn = RUN_COMMAND_EXIT_CODE;
    }

    //If the command line was to call for compilation this execution will trigger it.
    if (execution == null || !execution.run()) {
      // Check if compilation failed anywhere in the execution chain
      if (compilationContext.compilationResult().hasCompilationFailed()) {
        reporter.report("Compilation failed");
        rtn = COMPILATION_FAILED_EXIT_CODE;
      } else {
        reporter.report("Command not executed");
        rtn = BAD_COMMANDLINE_EXIT_CODE;
      }
    }

    return rtn;
  }

  private void deleteFinalArtifactIfDependenciesAltered(final CompilationContext compilationContext) {

    if (compilationContext.commandLine().isDependenciesAltered()) {
      reporter.log("Dependencies altered.");
      compilationContext.sourceFileCache().deleteTargetExecutableArtefact();
    }

  }

  private E getExecutionForDeveloperManagementOption(final CompilationContext compilationContext) {

    if (compilationContext.commandLine().options().isGenerateSigningKeys()) {
      return new Egk(compilationContext);
    } else if (compilationContext.commandLine().options().isUpdateUpgrade()) {
      return new Up(compilationContext);
    }

    return null;
  }

  private E getExecutionForBuildTypeOption(CompilationContext compilationContext) {

    if (compilationContext.commandLine().options().isCleanAll()) {
      return new Ecl(compilationContext);
    } else if (compilationContext.commandLine().options().isResolveDependencies()) {
      return new Edp(compilationContext);
    } else if (compilationContext.commandLine().options().isIncrementalCompile()) {
      return new Eic(compilationContext);
    } else if (compilationContext.commandLine().options().isFullCompile()) {
      return new Efc(compilationContext);
    } else if (compilationContext.commandLine().options().isPackaging()) {
      return new Ep(compilationContext);
    } else if (compilationContext.commandLine().options().isInstall()) {
      return new Ei(compilationContext);
    } else if (compilationContext.commandLine().options().isDeployment()) {
      return new Ed(compilationContext);
    }

    return null;
  }

  private E getExecutionForReleaseVectorOption(CompilationContext compilationContext) {

    if (compilationContext.commandLine().options().isIncrementReleaseVector()) {
      return new Eiv(compilationContext);
    } else if (compilationContext.commandLine().options().isSetReleaseVector()) {
      return new Esv(compilationContext);
    } else if (compilationContext.commandLine().options().isSetFeatureVector()) {
      return new Esf(compilationContext);
    } else if (compilationContext.commandLine().options().isPrintReleaseVector()) {
      return new Epv(compilationContext);
    }

    return null;
  }

  /**
   * Just used for reporting and logging errors and warnings.
   */
  private static class CompilationReporter extends Reporter {
    protected CompilationReporter(final boolean verbose) {

      super(verbose, false);

    }

    public void logPhaseCompilation(final CompilationEvent compilationEvent) {

      final var errorListener = compilationEvent.source().getErrorListener();
      log(String.format("%s: %s processed", compilationEvent.phase(), compilationEvent.source().getFileName()));

      if (errorListener.hasWarnings()) {
        report(String.format("%s: %s has warnings", compilationEvent.phase(), compilationEvent.source().getFileName()));
        errorListener.getWarnings().forEachRemaining(this::report);
      }

      if (errorListener.hasErrors()) {
        report(String.format("%s: Errors in %s", compilationEvent.phase(), compilationEvent.source().getFileName()));
        errorListener.getErrors().forEachRemaining(this::report);
      }

    }

    @Override
    protected String messagePrefix() {

      return "EK9     : ";
    }
  }
}
