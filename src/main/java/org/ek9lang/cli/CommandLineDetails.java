package org.ek9lang.cli;

import org.ek9lang.cli.support.EK9ProjectProperties;
import org.ek9lang.compiler.parsing.JustParser;
import org.ek9lang.core.exception.ExitException;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Just deals with handling the command line options for the compiler.
 * <p>
 * Quite a beast now, but command line argument handling is always a bit complex.
 */
public class CommandLineDetails
{
	private final OsSupport osSupport;
	private final FileHandling fileHandling;
	private final List<String> ek9AppParameters = new ArrayList<>();
	private final List<String> ek9AppDefines = new ArrayList<>();
	private final List<String> ek9ProgramParameters = new ArrayList<>();

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

	String ek9ProgramToRun = null;
	String targetArchitecture = "java";
	int debugPort = 8000;

	/**
	 * This is just a really simple cut down version of a visitor
	 * that just deals with packages.
	 */
	private EK9SourceVisitor visitor = null;

	public CommandLineDetails(FileHandling fileHandling, OsSupport osSupport)
	{
		this.fileHandling = fileHandling;
		this.osSupport = osSupport;
	}

	public OsSupport getOsSupport()
	{
		return osSupport;
	}

	public FileHandling getFileHandling()
	{
		return fileHandling;
	}

	/**
	 * Process the command line as supplied from main.
	 * Expects a single entry in the array.
	 */
	public int processCommandLine(String[] argv)
	{
		if(argv == null || argv.length == 0)
		{
			showHelp();
			return EK9.BAD_COMMANDLINE_EXIT_CODE;
		}

		return processCommandLine(argv[0]);
	}

	/**
	 * Process the command line.
	 *
	 * @param commandLine The command line the user or remote system used.
	 * @return error code, 0 no error - see EK9.java for the use of other error codes.
	 */
	public int processCommandLine(String commandLine)
	{
		try
		{
			if(commandLine == null || commandLine.isEmpty())
			{
				showHelp();
				return EK9.BAD_COMMANDLINE_EXIT_CODE;
			}

			processDefaultArchitecture();

			int returnCode = extractCommandLineDetails(commandLine);

			if(returnCode == EK9.RUN_COMMAND_EXIT_CODE)
				return assessCommandLine(commandLine);

			return returnCode;
		}
		catch(ExitException exitException)
		{
			System.err.println(exitException);
			return exitException.getExitCode();
		}
	}

	/**
	 * Extract the details of the command line out.
	 * There are some basic checks here, but only up to a point.
	 * The assessCommandLine checks the combination of commands.
	 */
	private int extractCommandLineDetails(String commandLine)
	{
		boolean processingEK9Parameters = true;
		List<String> activeParameters = ek9AppParameters;
		//Need to break this up remove extra spaces unless in quotes.
		String[] strArray = commandLine.split(" +(?=([^']*'[^']*')*[^']*$)");

		for(int i = 0; i < strArray.length; i++)
		{
			if(strArray[i].endsWith("ek9") && !strArray[i].contains("="))
			{
				foundEk9File = true;
				String ek9SourceFileName = strArray[i];
				mainSourceFile = new File(ek9SourceFileName);
				//We need to look in the current directory instead
				if(!mainSourceFile.exists())
					mainSourceFile = new File(osSupport.getCurrentWorkingDirectory(), ek9SourceFileName);

				processingEK9Parameters = false;
				activeParameters = ek9ProgramParameters;
			}
			else if(strArray[i].equals("-e") && i < strArray.length - 1)
			{
				ek9AppDefines.add(strArray[++i].replaceAll("'", "\""));
			}
			else if(strArray[i].equals("-T") && i < strArray.length - 1)
			{
				targetArchitecture = strArray[++i].toLowerCase();
				if(!targetArchitecture.equals("java"))
				{
					System.err.println("Only Java is currently supported as a target [" + commandLine + "]");
					return EK9.BAD_COMMANDLINE_EXIT_CODE;
				}
			}
			else if(strArray[i].equals("-r") && i < strArray.length - 1)
			{
				//So next is program to run
				ek9ProgramToRun = strArray[++i];
			}
			else if(strArray[i].equals("-d") && i < strArray.length - 1)
			{
				if(!isAcceptableParameter(strArray[i]))
				{
					System.err.println("Incompatible command line options [" + commandLine + "]");
					return EK9.BAD_COMMAND_COMBINATION_EXIT_CODE;
				}
				activeParameters.add(strArray[i]);
				//So next is port to debug on
				String port = strArray[++i];
				activeParameters.add(port);
				if(!Pattern.compile("^(\\d+)$").matcher(port).find())
				{
					System.err.println("Debug Mode: expecting integer port number [" + commandLine + "]");
					return EK9.BAD_COMMANDLINE_EXIT_CODE;
				}
				this.debugPort = Integer.parseInt(port);
			}
			else
			{
				if(processingEK9Parameters && !isAcceptableParameter(strArray[i]))
				{
					System.err.println("Incompatible command line options [" + commandLine + "]");
					return EK9.BAD_COMMAND_COMBINATION_EXIT_CODE;
				}
				activeParameters.add(strArray[i]);
				//Need to consume next option.
				if(strArray[i].equals("-IV") || strArray[i].equals("-SV") || strArray[i].equals("-SF"))
				{
					String versioningOption = strArray[i];
					if(i < strArray.length - 1)
					{
						String versionParam = strArray[++i];
						if(versioningOption.equals("-IV"))
						{
							//must be one of major|minor|patch|build
							if(Eve.Version.isInvalidVersionAddressPart(versionParam))
							{
								System.err.println("Increment Version: expecting major|minor|patch|build [" + commandLine + "]");
								return EK9.BAD_COMMANDLINE_EXIT_CODE;
							}
						}
						else if(versioningOption.equals("-SV"))
						{
							Eve.Version.withNoFeatureNoBuildNumber(versionParam);
						}
						else
						{
							Eve.Version.withFeatureNoBuildNumber(versionParam);
						}
						activeParameters.add(versionParam);
					}
					else
					{
						System.err.println("Missing parameter [" + commandLine + "]");
						return EK9.BAD_COMMANDLINE_EXIT_CODE;
					}
				}
			}
		}
		//Looks like we're potentially good here, next needs assessment.
		return EK9.RUN_COMMAND_EXIT_CODE;
	}

	/**
	 * Assess the values extracted from the command line to see if they are valid and viable.
	 */
	private int assessCommandLine(String commandLine)
	{
		if(isHelp())
			return showHelp();

		if(isVersionOfEK9Option())
			return showVersionOfEK9();

		appendRunOptionIfNecessary();

		if(isDeveloperManagementOption() && foundEk9File)
		{
			System.err.println("A generate Keys does not require or use EK9 filename.");
			return EK9.BAD_COMMAND_COMBINATION_EXIT_CODE;
		}

		if(!isDeveloperManagementOption() && !isRunEK9AsLanguageServer())
		{
			if(!foundEk9File)
			{
				System.err.println("no EK9 file name in command line [" + commandLine + "]");
				return EK9.FILE_ISSUE_EXIT_CODE;
			}

			processEK9FileProperties(false);

			if(isJustABuildTypeOption() && ek9ProgramToRun != null)
			{
				System.err.println("A Build request for " + mainSourceFile.getName() + " does not require or use program parameters.");
				return EK9.BAD_COMMAND_COMBINATION_EXIT_CODE;
			}
			if(isReleaseVectorOption() && ek9ProgramToRun != null)
			{
				System.err.println("A modification to version number for " + mainSourceFile.getName() + " does not require or use program parameters.");
				return EK9.BAD_COMMAND_COMBINATION_EXIT_CODE;
			}

			if(isRunOption())
				return assessRunOptions();
		}
		//i.e. a command does need to run.
		return EK9.RUN_COMMAND_EXIT_CODE;
	}

	private int showHelp()
	{
		System.err.println("ek9 <options>");
		System.err.println(CommandLineDetails.getCommandLineHelp());
		//i.e. no further commands need to run
		return EK9.SUCCESS_EXIT_CODE;
	}

	private int showVersionOfEK9()
	{
		System.err.println("EK9 Version 0.0.1-0");
		//i.e. no further commands need to run
		return EK9.SUCCESS_EXIT_CODE;
	}

	private void appendRunOptionIfNecessary()
	{
		//Add in run mode if no options supplied as default.
		if(!isJustABuildTypeOption()
				&& !isReleaseVectorOption()
				&& !isDeveloperManagementOption()
				&& !isRunDebugMode()
				&& !isRunEK9AsLanguageServer()
				&& !isUnitTestExecution())
			ek9AppParameters.add("-r");
	}

	private int assessRunOptions()
	{
		//Check run options
		if(programs.isEmpty())
		{
			//There's nothing that can be run
			System.err.println(mainSourceFile.getName() + " does not contain any programs.");
			return EK9.NO_PROGRAMS_EXIT_CODE;
		}
		if(ek9ProgramToRun == null)
		{
			if(programs.size() == 1)
			{
				//We can default that in.
				ek9ProgramToRun = programs.get(0);
			}
			else
			{
				System.err.print("Use '-r' and select one of");
				programs.stream().map(progName -> " '" + progName + "'").forEach(System.err::print);
				System.err.println(" from source file " + mainSourceFile.getName());
				return EK9.PROGRAM_NOT_SPECIFIED_EXIT_CODE;
			}
		}
		//Check program name is actually in the list of programs
		if(!programs.contains(ek9ProgramToRun))
		{
			System.err.print("Program must be one of");
			programs.stream().map(progName -> " '" + progName + "'").forEach(System.err::print);
			System.err.println(", source file " + mainSourceFile.getName() + " does not have program '" + ek9ProgramToRun + "'");

			return EK9.BAD_COMMAND_COMBINATION_EXIT_CODE;
		}

		//i.e. a command does need to run.
		return EK9.RUN_COMMAND_EXIT_CODE;
	}

	/**
	 * Pick up any proposed architect from environment first
	 * This replaces the built-in default of 'java'
	 * But even this can be overridden on the command line with -T
	 */
	private void processDefaultArchitecture()
	{
		String proposedTargetArchitecture = System.getenv("EK9_TARGET");
		if(proposedTargetArchitecture != null && !proposedTargetArchitecture.isEmpty())
			this.targetArchitecture = proposedTargetArchitecture;
	}

	public Integer processEK9FileProperties(boolean forceRegeneration)
	{
		if(forceRegeneration)
			this.visitor = null;

		String projectEK9Directory = fileHandling.getDotEK9Directory(this.getSourceFileDirectory());
		fileHandling.validateEK9Directory(projectEK9Directory, this.targetArchitecture);
		fileHandling.validateHomeEK9Directory(this.targetArchitecture);

		return establishSourceProperties(mainSourceFile, forceRegeneration);
	}

	private Integer establishSourceProperties(File sourceFile, boolean forceRegeneration)
	{
		EK9ProjectProperties versionProperties = new EK9ProjectProperties(getSourcePropertiesFile());
		Integer rtn = null;
		if(versionProperties.exists())
		{
			Properties properties = versionProperties.loadProperties();
			//There will always be a moduleName - but the rest may not be present.
			moduleName = properties.getProperty("moduleName");

			depsFingerPrint = properties.getProperty("depsFingerPrint");
			String progs = properties.getProperty("programs");
			if(progs != null)
				programs = Arrays.asList(progs.split(","));

			String hasPackage = properties.getProperty("package");
			if(hasPackage != null)
				packagePresent = properties.getProperty("package").equals("true");

			String ver = properties.getProperty("version");
			if(ver != null)
				version = ver;
		}

		if(versionProperties.isNewerThan(sourceFile) && !forceRegeneration)
		{
			if(isVerbose())
				System.err.println("Props   : Reusing " + versionProperties.getFileName());
		}
		else
		{
			if(isVerbose())
				System.err.println("Props   : Regenerating " + versionProperties.getFileName());

			visitor = getSourceVisitor();
			moduleName = visitor.getModuleName();
			programs = visitor.getPrograms();
			packagePresent = visitor.isPackagePresent();
			version = visitor.getVersion();

			String oldFingerPrint = depsFingerPrint;
			depsFingerPrint = visitor.getDependencyFingerPrint();

			if(oldFingerPrint != null && !oldFingerPrint.equals(depsFingerPrint))
				dependenciesAltered = true;

			Properties properties = new Properties();
			properties.setProperty("sourceFile", sourceFile.getName());
			properties.setProperty("moduleName", moduleName);
			properties.setProperty("programs", versionProperties.prepareListForStorage(programs));
			properties.setProperty("depsFingerPrint", depsFingerPrint);

			if(version != null)
			{
				properties.setProperty("version", version);
				rtn = visitor.getVersionNumberOnLine();
			}
			properties.setProperty("package", Boolean.toString(packagePresent));
			versionProperties.storeProperties(properties);
		}

		return rtn;
	}

	public EK9SourceVisitor getSourceVisitor()
	{
		if(visitor == null)
		{
			visitor = new EK9SourceVisitor();
			if(!new JustParser().readSourceFile(mainSourceFile, visitor))
				throw new ExitException(EK9.FILE_ISSUE_EXIT_CODE, "Unable to Parse source file [" + mainSourceFile.getAbsolutePath() + "]");
		}

		return visitor;
	}

	public File getSourcePropertiesFile()
	{
		return fileHandling.getTargetPropertiesArtefact(mainSourceFile.getPath());
	}

	public int numberOfProgramsInSourceFile()
	{
		return programs.size();
	}

	public String getModuleName()
	{
		return moduleName;
	}

	public boolean isPackagePresent()
	{
		return packagePresent;
	}

	public String getVersion()
	{
		return version;
	}

	public boolean applyStandardIncludes()
	{
		return getSourceVisitor().isApplyStandardIncludes();
	}

	public boolean applyStandardExcludes()
	{
		return getSourceVisitor().isApplyStandardExcludes();
	}

	public List<String> getIncludeFiles()
	{
		return getSourceVisitor().getIncludeFiles();
	}

	//Will pick up from visitor when processing any package directives
	public List<String> getExcludeFiles()
	{
		return getSourceVisitor().getExcludeFiles();
	}

	public static String getCommandLineHelp()
	{
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
				\t-Cg Force full recompilation but with debugging information; but don't run
				\t-Cd Force full recompilation but with all dev code and debugging information; but don't run
				\t-Cl Clean all, including dependencies
				\t-Dp Resolve all dependencies, this triggers -Cl clean all first
				\t-P Package ready for deployment to artefact server/library. You decide if software is fit to package.
				\t-I Install packaged software to your local library.
				\t-Gk Generate signing keys for deploying packages to an artefact server
				\t-D Deploy packaged software to an artefact server, this triggers -P packaging first and -Gk if necessary
				\t-IV [major|minor|patch|build] - increment part of the release vector
				\t-SV major.minor.patch - force setting to specific value i.e 6.8.1-0 (always zeros build on setting)
				\t-SF major.minor.patch-feature - force setting to specific value i.e 6.8.0-specials-0 (always zeros build on setting)
				\t-PV print out the current version i.e 6.8.1-0 or 6.8.0-specials-0 for a feature.
				\t-v Verbose mode
				\t-t Runs all unit tests that have been found, this triggers -Cd full compile first
				\t-d port Run in debug mode (requires debugging information - on a port)
				\t-e <name>=<value> set an environment variable ie. user=Steve or user='Steve Limb' for spaces
				\t-T target architecture - defaults to 'java' if not specified.
				\tfilename.ek9 - the main file to work with
				\t-r Program to run (if EK9 file as more than one)
				""";
	}

	private boolean isAcceptableParameter(String param)
	{
		if(isModifierParam(param))
			return true;
		if(!isMainParam(param))
			return false;
		//only if we are not one of these already.
		if(isJustABuildTypeOption())
		{
			System.err.println("Option '" + param + "' not compatible with existing build option");
			return false;
		}
		if(isDeveloperManagementOption())
		{
			System.err.println("Option '" + param + "' not compatible with existing management option");
			return false;
		}
		if(isReleaseVectorOption())
		{
			System.err.println("Option '" + param + "' not compatible with existing release option");
			return false;
		}
		if(isRunOption())
		{
			System.err.println("Option '" + param + "' not compatible with existing run option");
			return false;
		}
		if(isUnitTestExecution())
		{
			System.err.println("Option '" + param + "' not compatible with existing unit test option");
			return false;
		}
		return true;
	}

	private boolean isMainParam(String param)
	{
		switch(param)
		{
			case "-c":
			case "-cg":
			case "-cd":
			case "-C":
			case "-Cg":
			case "-Cd":
			case "-Cl":
			case "-Dp":
			case "-t":
			case "-d":
			case "-P":
			case "-I":
			case "-Gk":
			case "-D":
			case "-IV":
			case "-SV":
			case "-SF":
			case "-PV":
				return true;
			default:
				System.err.println("Option '" + param + "' not understood");
				return false;
		}
	}

	public boolean isDependenciesAltered()
	{
		return dependenciesAltered;
	}

	private boolean isModifierParam(String param)
	{
		return switch(param)
				{
					case "-V", "-h", "-v", "-ls", "-lsh" -> true;
					default -> false;
				};
	}

	public boolean isDebuggingInstrumentation()
	{
		return isExecutionOptionPresent("-cg") ||
				isExecutionOptionPresent("-cd") ||
				isExecutionOptionPresent("-Cg") ||
				isExecutionOptionPresent("-Cd");
	}

	public boolean isDevBuild()
	{
		return isExecutionOptionPresent("-Cd") || isExecutionOptionPresent("-cd");
	}

	public boolean isExecutionOptionPresent(String option)
	{
		return this.ek9AppParameters.contains(option);
	}

	public boolean isJustABuildTypeOption()
	{
		return isCleanAll() ||
				isResolveDependencies() ||
				isIncrementalCompile() ||
				isFullCompile() ||
				isPackaging() ||
				isInstall() ||
				isDeployment();
	}

	public boolean isReleaseVectorOption()
	{
		return isPrintReleaseVector() ||
				isIncrementReleaseVector() ||
				isSetReleaseVector() ||
				isSetFeatureVector();
	}

	public List<String> getEk9AppDefines()
	{
		return ek9AppDefines;
	}

	public List<String> getEk9ProgramParameters()
	{
		return ek9ProgramParameters;
	}

	public String getTargetArchitecture()
	{
		return targetArchitecture;
	}

	public String getProgramToRun()
	{
		//Might not have been set if there is just one program that is implicit.
		return ek9ProgramToRun;
	}

	/**
	 * This is just the file name and not the full path to the source file.
	 */
	public String getSourceFileName()
	{
		return mainSourceFile.getName();
	}

	/**
	 * Provides the full qualified path to the source file.
	 */
	public String getFullPathToSourceFileName()
	{
		return mainSourceFile.getPath();
	}

	public String getSourceFileDirectory()
	{
		return mainSourceFile.getParent();
	}

	public boolean isVerbose()
	{
		return this.ek9AppParameters.contains("-v");
	}

	public boolean isDeveloperManagementOption()
	{
		return isGenerateSigningKeys();
	}

	public boolean isGenerateSigningKeys()
	{
		return this.ek9AppParameters.contains("-Gk");
	}

	public boolean isCleanAll()
	{
		return this.ek9AppParameters.contains("-Cl");
	}

	public boolean isResolveDependencies()
	{
		return this.ek9AppParameters.contains("-Dp");
	}

	public boolean isIncrementalCompile()
	{
		return this.ek9AppParameters.contains("-c") || this.ek9AppParameters.contains("-cg") || this.ek9AppParameters.contains("-cd");
	}

	public boolean isFullCompile()
	{
		return this.ek9AppParameters.contains("-C") || this.ek9AppParameters.contains("-Cg") || this.ek9AppParameters.contains("-Cd");
	}

	public String getOptionParameter(String option)
	{
		int optionIndex = ek9AppParameters.indexOf(option);
		optionIndex++;
		if(optionIndex < ek9AppParameters.size())
			return ek9AppParameters.get(optionIndex);
		return null;
	}

	public boolean isInstall()
	{
		return this.ek9AppParameters.contains("-I");
	}

	public boolean isPackaging()
	{
		return this.ek9AppParameters.contains("-P");
	}

	public boolean isDeployment()
	{
		return this.ek9AppParameters.contains("-D");
	}

	public boolean isPrintReleaseVector()
	{
		return this.ek9AppParameters.contains("-PV");
	}

	public boolean isIncrementReleaseVector()
	{
		return this.ek9AppParameters.contains("-IV");
	}

	public boolean isSetReleaseVector()
	{
		return this.ek9AppParameters.contains("-SV");
	}

	public boolean isSetFeatureVector()
	{
		return this.ek9AppParameters.contains("-SF");
	}

	public boolean isHelp()
	{
		return this.ek9AppParameters.contains("-h");
	}

	public boolean isVersionOfEK9Option()
	{
		return this.ek9AppParameters.contains("-V");
	}

	public boolean isRunEK9AsLanguageServer()
	{
		return this.ek9AppParameters.contains("-ls");
	}

	public boolean isEK9LanguageServerHelpEnabled()
	{
		return this.ek9AppParameters.contains("-lsh");
	}

	public boolean isRunOption()
	{
		return isRunDebugMode() || isRunNormalMode();
	}

	public boolean isUnitTestExecution()
	{
		return this.ek9AppParameters.contains("-t");
	}

	public boolean isRunDebugMode()
	{
		return this.ek9AppParameters.contains("-d");
	}

	public boolean isRunNormalMode()
	{
		return this.ek9AppParameters.contains("-r");
	}
}
