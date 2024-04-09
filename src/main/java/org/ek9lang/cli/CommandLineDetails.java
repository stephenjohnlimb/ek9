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
import java.util.Set;
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

/**
 * Just deals with handling the command line options for the compiler.
 * Quite a beast now, but command line argument handling is always a bit complex.
 */
final class CommandLineDetails {

  //Clone environment variables in, but also allow use to programmatically alter them.
  private static final Map<String, String> DEFAULTS = new HashMap<>(System.getenv());

  private final LanguageMetaData languageMetaData;
  private final OsSupport osSupport;
  private final FileHandling fileHandling;
  private final List<String> ek9AppParameters = new ArrayList<>();
  private final List<String> ek9AppDefines = new ArrayList<>();
  private final List<String> ek9ProgramParameters = new ArrayList<>();
  String ek9ProgramToRun = null;
  String targetArchitecture = "java";
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
  CommandLineDetails(final LanguageMetaData languageMetaData,
                     final FileHandling fileHandling,
                     final OsSupport osSupport) {

    this.languageMetaData = languageMetaData;
    this.fileHandling = fileHandling;
    this.osSupport = osSupport;

  }

  static void addDefaultSetting() {

    DEFAULTS.put("EK9_TARGET", "java");
  }

  /**
   * Just provides the commandline help text.
   */
  static String getCommandLineHelp() {

    return """
        where possible options include:
        \t-V The version of the compiler/runtime
        \t-ls Run compiler as Language Server
        \t-lsh Provide EK9 Language Help/Hover
        \t-h Help message
        \t-c Incremental compile; but don't run
        \t-ch Incremental compile; but don't link to final executable
        \t-cg Incremental compile but with debugging information; but don't run
        \t-cd Incremental compile include dev code and debugging information; but don't run
        \t-cdh Incremental compile; include dev code, but don't link to final executable
        \t-Cp [phase] Full recompilation; compile up to a specific phase
        \t-Cdp [phase] Full recompilation (include dev code); compile up to a specific phase
        \t-C Force full recompilation; but don't run
        \t-Ch Force full recompilation; but don't link to final executable
        \t-Cg Force full recompilation with debugging information; but don't run
        \t-Cd Force full recompilation include dev code and debugging information; but don't run
        \t-Cdh Force full recompilation; include dev code, but don't link to final executable
        \t-Cl Clean all, including dependencies
        \t-Dp Resolve all dependencies, this triggers -Cl clean all first
        \t-P Package ready for deployment to artefact server/library.
        \t-I Install packaged software to your local library.
        \t-Gk Generate signing keys for deploying packages to an artefact server
        \t-D Deploy packaged software, this triggers -P packaging first and -Gk if necessary
        \t-IV [major|minor|patch|build] - increment part of the release vector
        \t-SV major.minor.patch - setting to a value i.e 6.8.1-0 (zeros build)
        \t-SF major.minor.patch-feature - setting to a value i.e 6.8.0-specials-0 (zeros build)
        \t-PV print out the current version i.e 6.8.1-0 or 6.8.0-specials-0 for a feature.
        \t-Up Check for newer version of ek9 and upgrade if available.
        \t-v Verbose mode
        \t-dv Debug mode - produces internal debug information during compilation
        \t-t Runs all unit tests that have been found, this triggers -Cd full compile first
        \t-d port Run in debug mode (requires debugging information - on a port)
        \t-e <name>=<value> set environment variable i.e. user=Steve or user='Steve Limb' for spaces
        \t-T target architecture - defaults to 'java' if not specified.
        \tfilename.ek9 - the main file to work with
        \t-r Program to run (if EK9 file as more than one)
        """;
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
  int processCommandLine(final String[] argv) {

    if (argv == null || argv.length == 0) {
      showHelp();
      return Ek9.BAD_COMMANDLINE_EXIT_CODE;
    }
    final var fullCommandLine = String.join(" ", argv);

    return processCommandLine(fullCommandLine);
  }

  /**
   * Process the command line.
   *
   * @param commandLine The command line the user or remote system used.
   * @return error code, 0 no error - see EK9.java for the use of other error codes.
   */
  int processCommandLine(final String commandLine) {

    try {
      if (commandLine == null || commandLine.isEmpty()) {
        showHelp();
        return Ek9.BAD_COMMANDLINE_EXIT_CODE;
      }

      processDefaultArchitecture();

      final var returnCode = extractCommandLineDetails(commandLine);

      if (!targetArchitecture.equals("java")) {
        Logger.error("Only Java is currently supported as a target [" + commandLine + "]");
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
      targetArchitecture = strArray[index + 1].toLowerCase();
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

  private boolean processPhasedCompilationOption(final String[] strArray,
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
        return true;
      } catch (Exception ex) {
        var optionsToChooseFrom = Arrays
            .stream(CompilationPhase.values())
            .map(Enum::name)
            .collect(Collectors.joining(", "));
        Logger.error(String.format("Phased Compilation: expecting one of %s", optionsToChooseFrom));
      }
    }

    return false;
  }

  private boolean isVersioningOption(final String[] strArray, final int index) {

    return strArray[index].equals("-IV")
        || strArray[index].equals("-SV")
        || strArray[index].equals("-SF");
  }

  private boolean processVersioningOption(final String[] strArray,
                                          final int index,
                                          final List<String> activeParameters) {

    final var versioningOption = strArray[index];
    if (index < strArray.length - 1) {
      final var versionParam = strArray[index + 1];
      if (versioningOption.equals("-IV")) {
        if (Eve.Version.isInvalidVersionAddressPart(versionParam)) {
          Logger.error(
              "Increment Version: expecting major|minor|patch|build");
          return false;
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
      return false;
    }

    return true;
  }

  private boolean isDebugOption(final String[] strArray, final int index) {

    return strArray[index].equals("-d") && index < strArray.length - 1;
  }

  private boolean processDebugOption(final String[] strArray,
                                     final int index,
                                     final List<String> activeParameters) {

    if (isParameterUnacceptable(strArray[index])) {
      Logger.error("Incompatible command line options");
      return false;
    }
    activeParameters.add(strArray[index]);
    //So next is port to debug on
    final var port = strArray[index + 1];

    if (!Pattern.compile("^(\\d+)$").matcher(port).find()) {
      Logger.error("Debug Mode: expecting integer port number");
      return false;
    }
    activeParameters.add(port);
    this.debugPort = Integer.parseInt(port);

    return true;
  }

  private boolean isInvalidEk9Parameter(final boolean processingEk9Parameters, final String parameter) {

    return processingEk9Parameters && isParameterUnacceptable(parameter);
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
    List<String> activeParameters = ek9AppParameters;
    int index = 0;

    while (index < strArray.length) {
      if (processIfIndexIsEk9FileName(strArray, index)) {
        processingEk9Parameters = false;
        activeParameters = ek9ProgramParameters;
      } else if (processIfSimpleOption(strArray, index)) {
        index++;
      } else if (isDebugOption(strArray, index)) {
        if (!processDebugOption(strArray, index, activeParameters)) {
          return Ek9.BAD_COMMANDLINE_EXIT_CODE;
        }
        index++;
      } else if (isPhasedCompilation(strArray, index)) {
        if (!processPhasedCompilationOption(strArray, index, activeParameters)) {
          return Ek9.BAD_COMMANDLINE_EXIT_CODE;
        }
        index++;
      } else if (isInvalidEk9Parameter(processingEk9Parameters, strArray[index])) {
        Logger.error("Incompatible command line options");
        return Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE;
      } else if (isVersioningOption(strArray, index)) {
        if (!processVersioningOption(strArray, index, activeParameters)) {
          return Ek9.BAD_COMMANDLINE_EXIT_CODE;
        }
        index++;
      } else {
        activeParameters.add(strArray[index]);
      }
      index++;
    }

    //Looks like we're potentially good here, next needs assessment.
    return Ek9.RUN_COMMAND_EXIT_CODE;
  }

  private Optional<Integer> checkHelpOrVersion() {

    Optional<Integer> rtn = Optional.empty();
    if (isHelp()) {
      rtn = Optional.of(showHelp());
    } else if (isVersionOfEk9Option()) {
      rtn = Optional.of(showVersionOfEk9());
    }

    return rtn;
  }

  private Optional<Integer> checkInappropriateProgram() {

    final String errorSuffix =
        mainSourceFile.getName() + " does not require or use program parameters.";

    Optional<Integer> rtn = Optional.empty();
    if (ek9ProgramToRun != null) {
      if (isJustBuildTypeOption()) {
        Logger.error("A Build request for " + errorSuffix);
        rtn = Optional.of(Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE);
      }
      if (isReleaseVectorOption()) {
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

    appendRunOptionIfNecessary();

    if (isDeveloperManagementOption() && foundEk9File) {
      Logger.error("EK9 filename not required for this option.");
      return Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE;
    }

    if (!isDeveloperManagementOption() && !isRunEk9AsLanguageServer()) {
      if (!foundEk9File) {
        Logger.error("no EK9 file name in command line [" + commandLine + "]");
        return Ek9.FILE_ISSUE_EXIT_CODE;
      }

      processEk9FileProperties(false);

      final var inappropriateProgramUse = checkInappropriateProgram();
      if (inappropriateProgramUse.isPresent()) {
        return inappropriateProgramUse.get();
      }

      if (isRunOption()) {
        return assessRunOptions();
      }
    }

    //i.e. a command does need to run.
    return Ek9.RUN_COMMAND_EXIT_CODE;
  }

  private int showHelp() {

    Logger.error("ek9 <options>");
    Logger.error(CommandLineDetails.getCommandLineHelp());
    //i.e. no further commands need to run

    return Ek9.SUCCESS_EXIT_CODE;
  }

  private int showVersionOfEk9() {

    Logger.error("EK9 Version " + getLanguageMetaData().version());
    //i.e. no further commands need to run

    return Ek9.SUCCESS_EXIT_CODE;
  }

  private void appendRunOptionIfNecessary() {

    //Add in run mode if no options supplied as default.
    if (!isJustBuildTypeOption() && !isReleaseVectorOption() && !isDeveloperManagementOption()
        && !isRunDebugMode() && !isRunEk9AsLanguageServer() && !isUnitTestExecution()) {
      ek9AppParameters.add("-r");
    }

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
        ek9ProgramToRun = programs.get(0);
      } else {
        var builder = new StringBuilder("Use '-r' and select one of");
        programs.stream().map(progName -> " '" + progName + "'").forEach(builder::append);
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
   * This replaces the built-in default of 'java'
   * But even this can be overridden on the command line with -T
   */
  private void processDefaultArchitecture() {

    final var proposedTargetArchitecture = DEFAULTS.get("EK9_TARGET");
    if (proposedTargetArchitecture != null && !proposedTargetArchitecture.isEmpty()) {
      this.targetArchitecture = proposedTargetArchitecture;
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
      if (isVerbose()) {
        Logger.error("Props   : Regenerating " + versionProperties.getFileName());
      }
      rtn = reprocessProperties(sourceFile, versionProperties);
    } else {
      if (isVerbose()) {
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

  private boolean isParameterUnacceptable(final String param) {

    if (isModifierParam(param)) {
      return false;
    }

    final var builder = new StringBuilder("Option '").append(param);

    if (!isMainParam(param)) {
      Logger.error(builder.append("' not understood"));
      return true;
    }
    //only if we are not one of these already.
    if (isJustBuildTypeOption()) {
      Logger.error(builder.append("' not compatible with existing build option"));
      return true;
    }
    if (isDeveloperManagementOption()) {
      Logger.error(builder.append("' not compatible with existing management option"));
      return true;
    }
    if (isReleaseVectorOption()) {
      Logger.error(builder.append("' not compatible with existing release option"));
      return true;
    }
    if (isRunOption()) {
      Logger.error(builder.append("' not compatible with existing run option"));
      return true;
    }
    if (isUnitTestExecution()) {
      Logger.error(builder.append("' not compatible with existing unit test option"));
      return true;
    }

    return false;
  }

  private boolean isMainParam(final String param) {

    return Set.of("-c", "-ch", "-cg", "-cd", "-cdh", "-Cp", "-Cdp", "-C", "-Ch", "-Cg", "-Cd",
        "-Cdh", "-Cl", "-Dp", "-t", "-d", "-P", "-I", "-Gk", "-D", "-IV", "-SV", "-SF", "-PV",
        "-Up").contains(param);
  }

  public boolean isDependenciesAltered() {

    return dependenciesAltered;
  }

  private boolean isModifierParam(String param) {

    return Set.of("-V", "-h", "-v", "-dv", "-ls", "-lsh").contains(param);
  }

  public boolean isDebuggingInstrumentation() {

    return isOptionPresentInAppParameters(
        Set.of("-cg", "-cd", "-cdh", "-Cg", "-Cd", "-Cdh", "-Cdp"));
  }

  public boolean isDevBuild() {

    return isOptionPresentInAppParameters(Set.of("-cd", "-cdh", "-Cd", "-Cdh", "-Cdp"));
  }

  public boolean isJustBuildTypeOption() {

    return isCleanAll() || isResolveDependencies() || isIncrementalCompile() || isFullCompile()
        || isPackaging() || isInstall() || isDeployment();
  }

  public boolean isReleaseVectorOption() {

    return isPrintReleaseVector() || isIncrementReleaseVector() || isSetReleaseVector()
        || isSetFeatureVector();
  }

  public List<String> getEk9AppDefines() {

    return ek9AppDefines;
  }

  public List<String> getEk9ProgramParameters() {

    return ek9ProgramParameters;
  }

  public String getTargetArchitecture() {

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

    return mainSourceFile.getParent();
  }

  public boolean isVerbose() {

    return isOptionPresentInAppParameters(Set.of("-v"));
  }

  public boolean isDebugVerbose() {

    return isOptionPresentInAppParameters(Set.of("-dv"));
  }

  public boolean isDeveloperManagementOption() {

    return isGenerateSigningKeys() || isUpdateUpgrade();
  }

  public boolean isGenerateSigningKeys() {

    return isOptionPresentInAppParameters(Set.of("-Gk"));
  }

  public boolean isUpdateUpgrade() {

    return isOptionPresentInAppParameters(Set.of("-Up"));
  }

  public boolean isCleanAll() {

    return isOptionPresentInAppParameters(Set.of("-Cl"));
  }

  public boolean isResolveDependencies() {

    return isOptionPresentInAppParameters(Set.of("-Dp"));
  }

  public boolean isIncrementalCompile() {

    return isOptionPresentInAppParameters(Set.of("-c", "-ch", "-cg", "-cd", "-cdh"));
  }

  public boolean isFullCompile() {

    return isOptionPresentInAppParameters(Set.of("-Cp", "-Cdp", "-C", "-Ch", "-Cg", "-Cd", "-Cdh"));
  }

  public boolean isCheckCompileOnly() {

    return isOptionPresentInAppParameters(Set.of("-Cp", "-Cdp", "-ch", "-cdh", "-Ch", "-Cdh"));
  }

  public boolean isPhasedCompileOnly() {

    return isOptionPresentInAppParameters(Set.of("-Cp", "-Cdp"));
  }

  /**
   * Access a parameter option from the command line.
   */
  public String getOptionParameter(final String option) {

    String rtn = null;
    int optionIndex = ek9AppParameters.indexOf(option);
    optionIndex++;
    if (optionIndex < ek9AppParameters.size()) {
      rtn = ek9AppParameters.get(optionIndex);
    }

    return rtn;
  }

  public boolean isInstall() {

    return isOptionPresentInAppParameters(Set.of("-I"));
  }

  public boolean isPackaging() {

    return isOptionPresentInAppParameters(Set.of("-P"));
  }

  public boolean isDeployment() {

    return isOptionPresentInAppParameters(Set.of("-D"));
  }

  public boolean isPrintReleaseVector() {

    return isOptionPresentInAppParameters(Set.of("-PV"));
  }

  public boolean isIncrementReleaseVector() {

    return isOptionPresentInAppParameters(Set.of("-IV"));
  }

  public boolean isSetReleaseVector() {

    return isOptionPresentInAppParameters(Set.of("-SV"));
  }

  public boolean isSetFeatureVector() {

    return isOptionPresentInAppParameters(Set.of("-SF"));
  }

  public boolean isHelp() {

    return isOptionPresentInAppParameters(Set.of("-h"));
  }

  public boolean isVersionOfEk9Option() {

    return isOptionPresentInAppParameters(Set.of("-V"));
  }

  public boolean isRunEk9AsLanguageServer() {

    return isOptionPresentInAppParameters(Set.of("-ls")) || isEk9LanguageServerHelpEnabled();
  }

  public boolean isEk9LanguageServerHelpEnabled() {

    return isOptionPresentInAppParameters(Set.of("-lsh"));
  }

  public boolean isRunOption() {

    return isRunDebugMode() || isRunNormalMode();
  }

  public boolean isUnitTestExecution() {

    return isOptionPresentInAppParameters(Set.of("-t"));
  }

  public boolean isRunDebugMode() {

    return isOptionPresentInAppParameters(Set.of("-d"));
  }

  public boolean isRunNormalMode() {

    return isOptionPresentInAppParameters(Set.of("-r"));
  }

  private boolean isOptionPresentInAppParameters(final Set<String> options) {

    return options.stream().anyMatch(ek9AppParameters::contains);
  }
}
