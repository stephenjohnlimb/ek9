package org.ek9lang.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import org.ek9lang.LanguageMetaData;
import org.ek9lang.cli.support.Ek9ProjectProperties;
import org.ek9lang.cli.support.Ek9SourceVisitor;
import org.ek9lang.cli.support.PackageDetails;
import org.ek9lang.compiler.parsing.JustParser;
import org.ek9lang.core.exception.ExitException;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.Logger;
import org.ek9lang.core.utils.OsSupport;

/**
 * Just deals with handling the command line options for the compiler.
 * Quite a beast now, but command line argument handling is always a bit complex.
 */
public class CommandLineDetails {
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
  public CommandLineDetails(LanguageMetaData languageMetaData, FileHandling fileHandling,
                            OsSupport osSupport) {
    this.languageMetaData = languageMetaData;
    this.fileHandling = fileHandling;
    this.osSupport = osSupport;
  }

  /**
   * Just provides the commandline help text.
   */
  public static String getCommandLineHelp() {
    return """
        where possible options include:
        \t-V The version of the compiler/runtime
        \t-ls Run compiler as Language Server
        \t-lsh Provide EK9 Language Help/Hover
        \t-h Help message
        \t-c Incremental compile; but don't run
        \t-cg Incremental compile but with debugging information; but don't run
        \t-cd Incremental compile but with all dev code and debugging information; but don't run
        \t-C Force full recompilation; but don't run
        \t-Cg Force full recompilation with debugging information; but don't run
        \t-Cd Force full recompilation with all dev code and debugging information; but don't run
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
        \t-v Verbose mode
        \t-t Runs all unit tests that have been found, this triggers -Cd full compile first
        \t-d port Run in debug mode (requires debugging information - on a port)
        \t-e <name>=<value> set environment variable i.e. user=Steve or user='Steve Limb' for spaces
        \t-T target architecture - defaults to 'java' if not specified.
        \tfilename.ek9 - the main file to work with
        \t-r Program to run (if EK9 file as more than one)
        """;
  }

  public OsSupport getOsSupport() {
    return osSupport;
  }

  public FileHandling getFileHandling() {
    return fileHandling;
  }

  public LanguageMetaData getLanguageMetaData() {
    return languageMetaData;
  }

  /**
   * Process the command line as supplied from main.
   * Expects a single entry in the array.
   */
  public int processCommandLine(String[] argv) {
    if (argv == null || argv.length == 0) {
      showHelp();
      return Ek9.BAD_COMMANDLINE_EXIT_CODE;
    }

    return processCommandLine(argv[0]);
  }

  /**
   * Process the command line.
   *
   * @param commandLine The command line the user or remote system used.
   * @return error code, 0 no error - see EK9.java for the use of other error codes.
   */
  public int processCommandLine(String commandLine) {
    try {
      if (commandLine == null || commandLine.isEmpty()) {
        showHelp();
        return Ek9.BAD_COMMANDLINE_EXIT_CODE;
      }

      processDefaultArchitecture();

      int returnCode = extractCommandLineDetails(commandLine);

      if (returnCode == Ek9.RUN_COMMAND_EXIT_CODE) {
        return assessCommandLine(commandLine);
      }

      return returnCode;
    } catch (ExitException exitException) {
      Logger.error(exitException);
      return exitException.getExitCode();
    }
  }

  /**
   * Extract the details of the command line out.
   * There are some basic checks here, but only up to a point.
   * The assessCommandLine checks the combination of commands.
   * A bit nasty in terms of processing the options. Take some time to focus here.
   * TODO make it simpler!
   */
  private int extractCommandLineDetails(String commandLine) {
    boolean processingEk9Parameters = true;
    List<String> activeParameters = ek9AppParameters;
    //Need to break this up remove extra spaces unless in quotes.
    String[] strArray = commandLine.split(" +(?=([^']*+'[^']*+')*+[^']*+$)");

    for (int i = 0; i < strArray.length; i++) {
      if (strArray[i].endsWith("ek9") && !strArray[i].contains("=")) {
        foundEk9File = true;
        String ek9SourceFileName = strArray[i];
        mainSourceFile = new File(ek9SourceFileName);
        //We need to look in the current directory instead
        if (!mainSourceFile.exists()) {
          mainSourceFile = new File(osSupport.getCurrentWorkingDirectory(), ek9SourceFileName);
        }

        processingEk9Parameters = false;
        activeParameters = ek9ProgramParameters;
      } else if (strArray[i].equals("-e") && i < strArray.length - 1) {
        ek9AppDefines.add(strArray[++i].replace("'", "\""));
      } else if (strArray[i].equals("-T") && i < strArray.length - 1) {
        targetArchitecture = strArray[++i].toLowerCase();
        if (!targetArchitecture.equals("java")) {
          Logger.error("Only Java is currently supported as a target [" + commandLine + "]");
          return Ek9.BAD_COMMANDLINE_EXIT_CODE;
        }
      } else if (strArray[i].equals("-r") && i < strArray.length - 1) {
        //So next is program to run
        ek9ProgramToRun = strArray[++i];
      } else if (strArray[i].equals("-d") && i < strArray.length - 1) {
        if (isParameterUnacceptable(strArray[i])) {
          Logger.error("Incompatible command line options [" + commandLine + "]");
          return Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE;
        }
        activeParameters.add(strArray[i]);
        //So next is port to debug on
        String port = strArray[++i];
        activeParameters.add(port);
        if (!Pattern.compile("^(\\d+)$").matcher(port).find()) {
          Logger.error("Debug Mode: expecting integer port number [" + commandLine + "]");
          return Ek9.BAD_COMMANDLINE_EXIT_CODE;
        }
        this.debugPort = Integer.parseInt(port);
      } else {
        if (processingEk9Parameters && isParameterUnacceptable(strArray[i])) {
          Logger.error("Incompatible command line options [" + commandLine + "]");
          return Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE;
        }
        activeParameters.add(strArray[i]);
        //Need to consume next option.
        if (strArray[i].equals("-IV") || strArray[i].equals("-SV") || strArray[i].equals("-SF")) {
          String versioningOption = strArray[i];
          if (i < strArray.length - 1) {
            String versionParam = strArray[++i];
            if (versioningOption.equals("-IV")) {
              //must be one of major|minor|patch|build
              if (Eve.Version.isInvalidVersionAddressPart(versionParam)) {
                Logger.error(
                    "Increment Version: expecting major|minor|patch|build [" + commandLine + "]");
                return Ek9.BAD_COMMANDLINE_EXIT_CODE;
              }
            } else if (versioningOption.equals("-SV")) {
              Eve.Version.withNoFeatureNoBuildNumber(versionParam);
            } else {
              Eve.Version.withFeatureNoBuildNumber(versionParam);
            }
            activeParameters.add(versionParam);
          } else {
            Logger.error("Missing parameter [" + commandLine + "]");
            return Ek9.BAD_COMMANDLINE_EXIT_CODE;
          }
        }
      }
    }
    //Looks like we're potentially good here, next needs assessment.
    return Ek9.RUN_COMMAND_EXIT_CODE;
  }

  /**
   * Assess the values extracted from the command line to see if they are valid and viable.
   */
  private int assessCommandLine(String commandLine) {
    if (isHelp()) {
      return showHelp();
    }

    if (isVersionOfEk9Option()) {
      return showVersionOfEk9();
    }

    appendRunOptionIfNecessary();

    if (isDeveloperManagementOption() && foundEk9File) {
      Logger.error("A generate Keys does not require or use EK9 filename.");
      return Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE;
    }

    if (!isDeveloperManagementOption() && !isRunEk9AsLanguageServer()) {
      if (!foundEk9File) {
        Logger.error("no EK9 file name in command line [" + commandLine + "]");
        return Ek9.FILE_ISSUE_EXIT_CODE;
      }

      processEk9FileProperties(false);

      if (isJustBuildTypeOption() && ek9ProgramToRun != null) {
        Logger.error("A Build request for "
            + mainSourceFile.getName() + " does not require or use program parameters.");
        return Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE;
      }
      if (isReleaseVectorOption() && ek9ProgramToRun != null) {
        Logger.error("A modification to version number for "
            + mainSourceFile.getName() + " does not require or use program parameters.");
        return Ek9.BAD_COMMAND_COMBINATION_EXIT_CODE;
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
      programs.stream()
          .map(programName -> " '" + programName + "'")
          .forEach(builder::append);

      builder.append(", source file ")
          .append(mainSourceFile.getName())
          .append(" does not have program '")
          .append(ek9ProgramToRun).append("'");

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
    String proposedTargetArchitecture = System.getenv("EK9_TARGET");
    if (proposedTargetArchitecture != null && !proposedTargetArchitecture.isEmpty()) {
      this.targetArchitecture = proposedTargetArchitecture;
    }
  }

  /**
   * Processes the properties and forces regeneration if required.
   */
  public Integer processEk9FileProperties(boolean forceRegeneration) {
    if (forceRegeneration) {
      this.visitor = null;
    }

    String projectEk9Directory = fileHandling.getDotEk9Directory(this.getSourceFileDirectory());
    fileHandling.validateEk9Directory(projectEk9Directory, this.targetArchitecture);
    fileHandling.validateHomeEk9Directory(this.targetArchitecture);

    return establishSourceProperties(mainSourceFile, forceRegeneration);
  }

  private Integer establishSourceProperties(File sourceFile, boolean forceRegeneration) {
    Ek9ProjectProperties versionProperties = new Ek9ProjectProperties(getSourcePropertiesFile());
    Integer rtn = null;
    if (versionProperties.exists()) {
      Properties properties = versionProperties.loadProperties();
      //There will always be a moduleName - but the rest may not be present.
      moduleName = properties.getProperty("moduleName");

      depsFingerPrint = properties.getProperty("depsFingerPrint");
      String thePrograms = properties.getProperty("programs");
      if (thePrograms != null) {
        this.programs = Arrays.asList(thePrograms.split(","));
      }

      String hasPackage = properties.getProperty("package");
      if (hasPackage != null) {
        packagePresent = hasPackage.equals("true");
      }

      String ver = properties.getProperty("version");
      if (ver != null) {
        version = ver;
      }
    }

    if (versionProperties.isNewerThan(sourceFile) && !forceRegeneration) {
      if (isVerbose()) {
        Logger.error("Props   : Reusing " + versionProperties.getFileName());
      }
    } else {
      if (isVerbose()) {
        Logger.error("Props   : Regenerating " + versionProperties.getFileName());
      }

      visitor = getSourceVisitor();
      var optionalDetails = visitor.getPackageDetails();

      if (optionalDetails.isPresent()) {
        var packageDetails = optionalDetails.get();
        moduleName = packageDetails.moduleName();
        programs = packageDetails.programs();
        packagePresent = packageDetails.packagePresent();
        version = packageDetails.version();

        String oldFingerPrint = depsFingerPrint;
        depsFingerPrint = packageDetails.dependencyFingerPrint();

        if (oldFingerPrint != null && !oldFingerPrint.equals(depsFingerPrint)) {
          dependenciesAltered = true;
        }

        Properties properties = new Properties();
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
    }

    return rtn;
  }

  /**
   * Access the visitor of the source code being parsed.
   */
  public Ek9SourceVisitor getSourceVisitor() {
    if (visitor == null) {
      visitor = new Ek9SourceVisitor();
      if (!new JustParser().readSourceFile(mainSourceFile, visitor)) {
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

  private boolean isParameterUnacceptable(String param) {
    if (isModifierParam(param)) {
      return false;
    }
    var builder = new StringBuilder("Option '").append(param);

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

  private boolean isMainParam(String param) {
    return Set.of("-c", "-cg", "-cd", "-C", "-Cg", "-Cd", "-Cl", "-Dp", "-t", "-d", "-P", "-I",
        "-Gk", "-D", "-IV", "-SV", "-SF", "-PV").contains(param);
  }

  public boolean isDependenciesAltered() {
    return dependenciesAltered;
  }

  private boolean isModifierParam(String param) {
    return Set.of("-V", "-h", "-v", "-ls", "-lsh").contains(param);
  }

  public boolean isDebuggingInstrumentation() {
    return isOptionPresentInAppParameters(Set.of("-cg", "-cd", "-Cg", "-Cd"));
  }

  public boolean isDevBuild() {
    return isOptionPresentInAppParameters(Set.of("-Cd", "-cd"));
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

  public boolean isDeveloperManagementOption() {
    return isGenerateSigningKeys();
  }

  public boolean isGenerateSigningKeys() {
    return isOptionPresentInAppParameters(Set.of("-Gk"));
  }

  public boolean isCleanAll() {
    return isOptionPresentInAppParameters(Set.of("-Cl"));
  }

  public boolean isResolveDependencies() {
    return isOptionPresentInAppParameters(Set.of("-Dp"));
  }

  public boolean isIncrementalCompile() {
    return isOptionPresentInAppParameters(Set.of("-c", "-cg", "-cd"));
  }

  public boolean isFullCompile() {
    return isOptionPresentInAppParameters(Set.of("-C", "-Cg", "-Cd"));
  }

  /**
   * Access a parameter option from the command line.
   */
  public String getOptionParameter(String option) {
    int optionIndex = ek9AppParameters.indexOf(option);
    optionIndex++;
    if (optionIndex < ek9AppParameters.size()) {
      return ek9AppParameters.get(optionIndex);
    }
    return null;
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

  private boolean isOptionPresentInAppParameters(Set<String> options) {
    return options.stream().anyMatch(ek9AppParameters::contains);
  }
}
