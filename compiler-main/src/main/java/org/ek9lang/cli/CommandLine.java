package org.ek9lang.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.Ek9SourceVisitor;
import org.ek9lang.compiler.common.JustParser;
import org.ek9lang.compiler.common.PackageDetails;
import org.ek9lang.core.ExitException;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.Logger;
import org.ek9lang.core.OsSupport;
import org.ek9lang.core.ParentDirectoryForFile;
import org.ek9lang.core.TargetArchitecture;

/**
 * Deals with handling the command line options for the compiler.
 * Quite a beast now, but command line argument handling is always a bit complex.
 */
final class CommandLine {

  //Clone environment variables in, but also allow use to programmatically alter them.
  private static final Map<String, String> DEFAULTS = new HashMap<>(System.getenv());
  private static final CommandLineHelp commandLineHelp = new CommandLineHelp();
  private final ParentDirectoryForFile parentDirectoryForFile = new ParentDirectoryForFile();
  private final CommandLineOptions options = new CommandLineOptions();
  private final LanguageMetaData languageMetaData;
  private final OsSupport osSupport;
  private final FileHandling fileHandling;
  private final List<String> ek9AppDefines = new ArrayList<>();
  String ek9ProgramToRun = null;
  TargetArchitecture targetArchitecture = TargetArchitecture.JVM;
  int debugPort = 8000;
  private File mainSourceFile;
  private String moduleName = null;
  private List<String> programs = new ArrayList<>();
  //Might not actually get set if the file we are dealing with does not have
  //a package and hence no version declaration.
  private String version = "";
  //If we have a package then this is the finger-print of all the dependencies.
  //We can then check if they get altered.
  private String depsFingerPrint = null;
  private boolean dependenciesAltered = false;
  private boolean packagePresent = false;
  private boolean foundEk9File = false;

  /**
   * This is just a really simple cut down version of a visitor
   * that just deals with packages.
   */
  private Ek9SourceVisitor visitor = null;

  /**
   * Create a new command line details object.
   */
  CommandLine(final LanguageMetaData languageMetaData,
              final FileHandling fileHandling,
              final OsSupport osSupport) {

    this.languageMetaData = languageMetaData;
    this.fileHandling = fileHandling;
    this.osSupport = osSupport;

  }

  /**
   * Just provides the commandline help text.
   */
  static String getCommandLineHelp() {
    return commandLineHelp.helpText();
  }

  OsSupport getOsSupport() {

    return osSupport;
  }

  FileHandling getFileHandling() {

    return fileHandling;
  }

  LanguageMetaData getLanguageMetaData() {

    return languageMetaData;
  }

  /**
   * Process the command line as supplied from main.
   * Expects a single entry in the array.
   */
  int process(final String[] argv) {

    if (argv == null || argv.length == 0) {
      showHelp();
      return Ek9.BAD_COMMANDLINE_EXIT_CODE;
    }
    final var fullCommandLine = String.join(" ", argv);

    return process(fullCommandLine);
  }

  /**
   * Process the command line.
   *
   * @param commandLine The command line the user or remote system used.
   * @return error code, 0 no error - see EK9.java for the use of other error codes.
   */
  int process(final String commandLine) {

    try {
      if (commandLine == null || commandLine.isEmpty()) {
        showHelp();
        return Ek9.BAD_COMMANDLINE_EXIT_CODE;
      }

      processDefaultArchitecture();

      final var returnCode = extractCommandLineDetails(commandLine);

      if (targetArchitecture.equals(TargetArchitecture.NOT_SUPPORTED)) {
        Logger.error("Target Architecture [" + commandLine + "] not supported");
        return Ek9.BAD_COMMANDLINE_EXIT_CODE;
      }

      if (returnCode == Ek9.RUN_COMMAND_EXIT_CODE) {
        return assessCommandLine(commandLine);
      }

      return returnCode;
    } catch (ExitException exitException) {
      Logger.error(exitException);
      return exitException.getExitCode();
    }

  }

  public CommandLineOptions options() {

    return options;

  }

  private boolean processIfIndexIsEk9FileName(final String[] strArray, final int index) {

    final var rtn = strArray[index].endsWith("ek9") && !strArray[index].contains("=");

    if (rtn) {
      final var ek9SourceFileName = strArray[index];

      foundEk9File = true;
      mainSourceFile = new File(ek9SourceFileName);
      //We need to look in the current directory instead
      if (!mainSourceFile.exists()) {
        mainSourceFile = new File(osSupport.getCurrentWorkingDirectory(), ek9SourceFileName);
      }
    }

    return rtn;
  }

  private boolean processIfSimpleOption(final String[] strArray, final int index) {

    return processIfEnvironmentVariable(strArray, index)
        || processIfTargetArchitecture(strArray, index)
        || processIfProgramToRun(strArray, index);
  }

  private boolean processIfEnvironmentVariable(final String[] strArray, final int index) {

    final var rtn = strArray[index].equals("-e") && index < strArray.length - 1;

    if (rtn) {
      ek9AppDefines.add(strArray[index + 1].replace("'", "\""));
    }

    return rtn;
  }

  private boolean processIfTargetArchitecture(final String[] strArray, final int index) {

    final var rtn = strArray[index].equals("-T") && index < strArray.length - 1;

    if (rtn) {
      var commandLineArchitecture = strArray[index + 1].toUpperCase();
      targetArchitecture = TargetArchitecture.from(commandLineArchitecture);
      if (targetArchitecture == TargetArchitecture.NOT_SUPPORTED) {
        Logger.error("Only jvm/llvm-cpp is currently supported as a target [" + commandLineArchitecture + "]");
      }

    }
    return rtn;
  }

  private boolean processIfProgramToRun(final String[] strArray, final int index) {

    final var rtn = strArray[index].equals("-r") && index < strArray.length - 1;

    if (rtn) {
      ek9ProgramToRun = strArray[index + 1];
    }

    return rtn;
  }

  private boolean isPhasedCompilation(final String[] strArray, final int index) {

    return strArray[index].equals("-Cp") || strArray[index].equals("-Cdp");
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  private int processPhasedCompilationOption(final String[] strArray,
                                             final int index,
                                             final List<String> activeParameters) {

    final var compileCommand = strArray[index];
    if (index < strArray.length - 1) {
      final var compilationPhaseOptionProvided = strArray[index + 1];
      //Let's check that the option provided is one of the enumeration values.
      try {
        CompilationPhase.valueOf(compilationPhaseOptionProvided);
        activeParameters.add(compileCommand);
        activeParameters.add(compilationPhaseOptionProvided);
        return Ek9.RUN_COMMAND_EXIT_CODE;
      } catch (Exception _) {
        var optionsToChooseFrom = Arrays
            .stream(CompilationPhase.values())
            .map(Enum::name)
            .collect(Collectors.joining(", "));
        Logger.error(String.format("Phased Compilation: expecting one of %s", optionsToChooseFrom));
      }
    }

    return Ek9.BAD_COMMANDLINE_EXIT_CODE;
  }

  private boolean isVersioningOption(final String[] strArray, final int index) {

    return strArray[index].equals("-IV")
        || strArray[index].equals("-SV")
        || strArray[index].equals("-SF");
  }

  private int processVersioningOption(final String[] strArray,
                                      final int index,
                                      final List<String> activeParameters) {

    final var versioningOption = strArray[index];
    if (index < strArray.length - 1) {
      final var versionParam = strArray[index + 1];
      if (versioningOption.equals("-IV")) {
        if (Eve.Version.isInvalidVersionAddressPart(versionParam)) {
          Logger.error(
              "Increment Version: expecting major|minor|patch|build");
          return Ek9.BAD_COMMANDLINE_EXIT_CODE;
        }
      } else if (versioningOption.equals("-SV")) {
        //If not valid a runtime exception will occur.
        Eve.Version.withNoFeatureNoBuildNumber(versionParam);
      } else {
        Eve.Version.withFeatureNoBuildNumber(versionParam);
      }
      activeParameters.add(strArray[index]);
      activeParameters.add(versionParam);
    } else {
      Logger.error("Missing versioning parameter");
      return Ek9.BAD_COMMANDLINE_EXIT_CODE;
    }

    return Ek9.RUN_COMMAND_EXIT_CODE;
  }

  private boolean isDebugOption(final String[] strArray, final int index) {

    return strArray[index].equals("-d") && index < strArray.length - 1;
  }

  private int processDebugOption(final String[] strArray,
                                 final int index,
                                 final List<String> activeParameters) {

    if (options.isParameterUnacceptable(strArray[index])) {
      Logger.error("Incompatible command line options");
      return Ek9.BAD_COMMANDLINE_EXIT_CODE;
    }
    activeParameters.add(strArray[index]);
    //So next is port to debug on
    final var port = strArray[index + 1];

    if (!Pattern.compile("^(\\d+)$").matcher(port).find()) {
      Logger.error("Debug Mode: expecting integer port number");
      return Ek9.BAD_COMMANDLINE_EXIT_CODE;
    }
    activeParameters.add(port);
    this.debugPort = Integer.parseInt(port);

    return Ek9.RUN_COMMAND_EXIT_CODE;
  }

  private boolean isInvalidEk9Parameter(final boolean processingEk9Parameters, final String parameter) {

    return processingEk9Parameters && options.isParameterUnacceptable(parameter);
  }

  /**
   * Extract the details of the command line out.
   * There are some basic checks here, but only up to a point.
   * The assessCommandLine checks the combination of commands.
   * A bit nasty in terms of processing the options.
   */
  @SuppressWarnings("java:S3776")
  private int extractCommandLineDetails(final String commandLine) {

    //Need to break this up remove extra spaces unless in quotes.
    final var strArray = commandLine.split(" +(?=([^']*+'[^']*+')*+[^']*+$)");

    boolean processingEk9Parameters = true;
    List<String> activeParameters = options.getEk9AppParameters();
    int index = 0;
    int returnCode = Ek9.RUN_COMMAND_EXIT_CODE;

    while (returnCode == Ek9.RUN_COMMAND_EXIT_CODE && index < strArray.length) {

      if (processIfIndexIsEk9FileName(strArray, index)) {
        processingEk9Parameters = false;
        activeParameters = options.getEk9ProgramParameters();
      } else if (processIfSimpleOption(strArray, index)) {
        index++;
      } else if (isDebugOption(strArray, index)) {
        returnCode = processDebugOption(strArray, index, activeParameters);
        index++;
      } else if (isPhasedCompilation(strArray, index)) {
        returnCode = processPhasedCompilationOption(strArray, index, activeParameters);
        index++;
      } else if (isInvalidEk9Parameter(processingEk9Parameters, strArray[index])) {
        Logger.error("Incompatible command line options");
        returnCode = Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE;
      } else if (isVersioningOption(strArray, index)) {
        returnCode = processVersioningOption(strArray, index, activeParameters);
        index++;
      } else {
        activeParameters.add(strArray[index]);
      }
      index++;

    }

    //Looks like we're potentially good here, next needs assessment.
    return returnCode;
  }

  private Optional<Integer> checkHelpOrVersion() {

    Optional<Integer> rtn = Optional.empty();
    if (options.isHelp()) {
      rtn = Optional.of(showHelp());
    } else if (options.isVersionOfEk9Option()) {
      rtn = Optional.of(showVersionOfEk9());
    }

    return rtn;
  }

  private Optional<Integer> checkInappropriateProgram() {

    final String errorSuffix =
        mainSourceFile.getName() + " does not require or use program parameters.";

    Optional<Integer> rtn = Optional.empty();
    if (ek9ProgramToRun != null) {
      if (options.isJustBuildTypeOption()) {
        Logger.error("A Build request for " + errorSuffix);
        rtn = Optional.of(Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE);
      }
      if (options.isReleaseVectorOption()) {
        Logger.error("A modification to version number for " + errorSuffix);
        rtn = Optional.of(Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE);
      }
    }

    return rtn;
  }

  /**
   * Assess the values extracted from the command line to see if they are valid and viable.
   */
  private int assessCommandLine(final String commandLine) {

    final var helpOrVersion = checkHelpOrVersion();
    if (helpOrVersion.isPresent()) {
      return helpOrVersion.get();
    }

    options.appendRunOptionIfNecessary();

    if (options.isDeveloperManagementOption() && foundEk9File) {
      Logger.error("EK9 filename not required for this option.");
      return Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE;
    }

    if (!options.isDeveloperManagementOption() && !options.isRunEk9AsLanguageServer()) {
      if (!foundEk9File) {
        Logger.error("no EK9 file name in command line [" + commandLine + "]");
        return Ek9.FILE_ISSUE_EXIT_CODE;
      }

      processEk9FileProperties(false);

      final var inappropriateProgramUse = checkInappropriateProgram();
      if (inappropriateProgramUse.isPresent()) {
        return inappropriateProgramUse.get();
      }

      if (options.isRunOption()) {
        return assessRunOptions();
      }
    }

    //i.e. a command does need to run.
    return Ek9.RUN_COMMAND_EXIT_CODE;
  }

  private int showHelp() {

    Logger.error("ek9 <options>");
    Logger.error(CommandLine.getCommandLineHelp());
    //i.e. no further commands need to run

    return Ek9.SUCCESS_EXIT_CODE;
  }

  private int showVersionOfEk9() {

    Logger.error("EK9 Version " + getLanguageMetaData().version());
    //i.e. no further commands need to run

    return Ek9.SUCCESS_EXIT_CODE;
  }

  private int assessRunOptions() {

    //Check run options
    if (programs.isEmpty()) {
      //There's nothing that can be run
      Logger.error(mainSourceFile.getName() + " does not contain any programs.");
      return Ek9.NO_PROGRAMS_EXIT_CODE;
    }
    if (ek9ProgramToRun == null) {
      if (programs.size() == 1) {
        //We can default that in.
        ek9ProgramToRun = programs.getFirst();
      } else {
        var builder = new StringBuilder("Use '-r' and select one of");
        programs.stream().map(programName -> " '" + programName + "'").forEach(builder::append);
        builder.append(" from source file ").append(mainSourceFile.getName());
        Logger.error(builder.toString());
        return Ek9.PROGRAM_NOT_SPECIFIED_EXIT_CODE;
      }
    }
    //Check program name is actually in the list of programs
    if (!programs.contains(ek9ProgramToRun)) {
      var builder = new StringBuilder("Program must be one of");
      programs.stream().map(programName -> " '" + programName + "'").forEach(builder::append);

      builder.append(", source file ").append(mainSourceFile.getName())
          .append(" does not have program '").append(ek9ProgramToRun).append("'");

      Logger.error(builder.toString());

      return Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE;
    }

    //i.e. a command does need to run.
    return Ek9.RUN_COMMAND_EXIT_CODE;
  }

  /**
   * Pick up any proposed architect from environment first.
   * This replaces the built-in default of 'JVM'
   * But even this can be overridden on the command line with -T
   */
  private void processDefaultArchitecture() {

    final var proposedTargetArchitecture = DEFAULTS.get("EK9_TARGET");
    if (proposedTargetArchitecture != null && !proposedTargetArchitecture.isEmpty()) {
      this.targetArchitecture = TargetArchitecture.from(proposedTargetArchitecture);
    }
  }

  /**
   * Processes the properties and forces regeneration if required.
   */
  public Integer processEk9FileProperties(final boolean forceRegeneration) {

    if (forceRegeneration) {
      this.visitor = null;
    }

    final var projectEk9Directory = fileHandling.getDotEk9Directory(this.getSourceFileDirectory());
    fileHandling.validateEk9Directory(projectEk9Directory, this.targetArchitecture);
    fileHandling.validateHomeEk9Directory(this.targetArchitecture);

    return establishSourceProperties(mainSourceFile, forceRegeneration);
  }

  private Integer establishSourceProperties(final File sourceFile, final boolean forceRegeneration) {

    final var versionProperties = new Ek9ProjectProperties(getSourcePropertiesFile());
    updateFromVersionProperties(versionProperties);

    Integer rtn = null;
    if (!versionProperties.isNewerThan(sourceFile) || forceRegeneration) {
      if (options.isVerbose()) {
        Logger.error("Props   : Regenerating " + versionProperties.getFileName());
      }
      rtn = reprocessProperties(sourceFile, versionProperties);
    } else {
      if (options.isVerbose()) {
        Logger.error("Props   : Reusing " + versionProperties.getFileName());
      }
    }

    return rtn;
  }

  private void updateFromVersionProperties(final Ek9ProjectProperties versionProperties) {

    if (versionProperties.exists()) {
      final var properties = versionProperties.loadProperties();
      //There will always be a moduleName/finger-print - but the rest may not be present.
      moduleName = properties.getProperty("moduleName");
      depsFingerPrint = properties.getProperty("depsFingerPrint");
      updatePrograms(properties);
      updatePackage(properties);
      updateVersion(properties);
    }

  }

  private void updatePrograms(final Properties properties) {

    final var thePrograms = properties.getProperty("programs");
    if (thePrograms != null) {
      this.programs = Arrays.asList(thePrograms.split(","));
    }

  }

  private void updatePackage(final Properties properties) {

    final var hasPackage = properties.getProperty("package");
    if (hasPackage != null) {
      packagePresent = hasPackage.equals("true");
    }

  }

  private void updateVersion(final Properties properties) {

    final var ver = properties.getProperty("version");
    if (ver != null) {
      version = ver;
    }

  }

  private Integer reprocessProperties(final File sourceFile, final Ek9ProjectProperties versionProperties) {
    Integer rtn = null;
    visitor = getSourceVisitor();
    final var optionalDetails = visitor.getPackageDetails();

    if (optionalDetails.isPresent()) {
      final var packageDetails = optionalDetails.get();
      moduleName = packageDetails.moduleName();
      programs = packageDetails.programs();
      packagePresent = packageDetails.packagePresent();
      version = packageDetails.version();

      final var oldFingerPrint = depsFingerPrint;
      depsFingerPrint = packageDetails.dependencyFingerPrint();

      if (oldFingerPrint != null && !oldFingerPrint.equals(depsFingerPrint)) {
        dependenciesAltered = true;
      }

      final var properties = new Properties();
      properties.setProperty("sourceFile", sourceFile.getName());
      properties.setProperty("moduleName", moduleName);
      properties.setProperty("programs", versionProperties.prepareListForStorage(programs));
      properties.setProperty("depsFingerPrint", depsFingerPrint);

      if (version != null) {
        properties.setProperty("version", version);
        rtn = packageDetails.versionNumberOnLine();
      }
      properties.setProperty("package", Boolean.toString(packagePresent));
      versionProperties.storeProperties(properties);
    }

    return rtn;
  }

  /**
   * Access the visitor of the source code being parsed.
   */
  public Ek9SourceVisitor getSourceVisitor() {

    if (visitor == null) {
      visitor = new Ek9SourceVisitor();
      if (!new JustParser(true).readSourceFile(mainSourceFile, visitor)) {
        throw new ExitException(Ek9.FILE_ISSUE_EXIT_CODE,
            "Unable to Parse source file [" + mainSourceFile.getAbsolutePath() + "]");
      }
    }

    return visitor;
  }

  public File getSourcePropertiesFile() {

    return fileHandling.getTargetPropertiesArtefact(mainSourceFile.getPath());
  }

  public int numberOfProgramsInSourceFile() {

    return programs.size();
  }

  public String getModuleName() {

    return moduleName;
  }

  public boolean noPackageIsPresent() {

    return !packagePresent;
  }

  public String getVersion() {

    return version;
  }

  public boolean applyStandardIncludes() {

    return getSourceVisitor().getPackageDetails().map(PackageDetails::applyStandardIncludes)
        .orElse(false);
  }

  public boolean applyStandardExcludes() {

    return getSourceVisitor().getPackageDetails().map(PackageDetails::applyStandardExcludes)
        .orElse(false);
  }

  public List<String> getIncludeFiles() {

    return getSourceVisitor().getPackageDetails().map(PackageDetails::includeFiles)
        .orElse(Collections.emptyList());
  }

  //Will pick up from visitor when processing any package directives
  public List<String> getExcludeFiles() {

    return getSourceVisitor().getPackageDetails().map(PackageDetails::excludeFiles)
        .orElse(Collections.emptyList());
  }

  public boolean isDependenciesAltered() {

    return dependenciesAltered;
  }

  public List<String> getEk9AppDefines() {

    return ek9AppDefines;
  }

  public List<String> getEk9ProgramParameters() {
    return options.getEk9ProgramParameters();
  }

  public TargetArchitecture getTargetArchitecture() {

    return targetArchitecture;
  }

  public String getProgramToRun() {

    //Might not have been set if there is just one program that is implicit.
    return ek9ProgramToRun;
  }

  /**
   * This is just the file name and not the full path to the source file.
   */
  public String getSourceFileName() {

    return mainSourceFile.getName();
  }

  /**
   * Provides the full qualified path to the source file.
   */
  public String getFullPathToSourceFileName() {

    return mainSourceFile.getPath();
  }

  public String getSourceFileDirectory() {
    return parentDirectoryForFile.apply(mainSourceFile);
  }

}
