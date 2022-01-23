package org.ek9lang.cli;

import org.ek9lang.cli.support.EK9ProjectProperties;
import org.ek9lang.compiler.parsing.JustParser;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Just deals with handling the command line options for the compiler.
 * <p>
 * TODO this is no where near finished yet, still need to add more functionality
 * TODO for example the visitor that can process any package directives.
 * TODO also deal with the properties processing and checking of .ek9 directories.
 */
public class CommandLineDetails
{
	private final OsSupport osSupport;
	private final FileHandling fileHandling;
	private final List<String> ek9AppParameters = new ArrayList<>();
	private final List<String> ek9AppDefines = new ArrayList<>();
	private final List<String> ek9ProgramParameters = new ArrayList<>();

	String ek9ProgramToRun = null;
	private File mainSourceFile;
	String targetArchitecture = "java";
	int debugPort = 8000;

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
	private EK9SourceVisitor visitor = null;

	public CommandLineDetails(FileHandling fileHandling, OsSupport osSupport)
	{
		this.fileHandling = fileHandling;
		this.osSupport = osSupport;
	}

	/**
	 * process the command line as supplied from main.
	 */
	public int processCommandLine(String[] argv)
	{
		if(argv == null || argv.length == 0)
		{
			System.err.println("ek9 <options>");
			System.err.println(CommandLineDetails.getCommandLineHelp());
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
		//Pick up any proposed architect from environment first
		//This replaces the built-in default of 'java'
		//But even this can be overridden on the command line with -T
		String proposedTargetArchitecture = System.getenv("EK9_TARGET");
		if(proposedTargetArchitecture != null && !proposedTargetArchitecture.isEmpty())
			this.targetArchitecture = proposedTargetArchitecture;

		if(commandLine == null || commandLine.isEmpty())
		{
			System.err.println("CommandLine is null/empty");
			return EK9.BAD_COMMANDLINE_EXIT_CODE;
		}

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
				if(!mainSourceFile.exists())
				{
					//We need to look in the current directory instead
					mainSourceFile = new File(osSupport.getCurrentWorkingDirectory(), ek9SourceFileName);
				}
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
				Pattern p = Pattern.compile("^(\\d+)$");
				Matcher m = p.matcher(port);
				if(!m.find())
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
							if(!versionParam.equals("major") &&
									!versionParam.equals("minor") &&
									!versionParam.equals("patch") &&
									!versionParam.equals("build"))
							{
								System.err.println("Increment Version: expecting major|minor|patch|build [" + commandLine + "]");
								return EK9.BAD_COMMANDLINE_EXIT_CODE;
							}
						}
						else if(versioningOption.equals("-SV"))
						{
							//So only expecting following major.minor.patch
							Pattern p = Pattern.compile("^(\\d+)(\\.)(\\d+)(\\.)(\\d+)$");
							Matcher m = p.matcher(versionParam);
							if(!m.find())
							{
								System.err.println("Set Version: expecting major.minor.patch [" + commandLine + "]");
								return EK9.BAD_COMMANDLINE_EXIT_CODE;
							}
						}
						else
						{
							//So only expecting following major.minor.patch-featureName
							Pattern p = Pattern.compile("^(\\d+)(\\.)(\\d+)(\\.)(\\d+)(-)([a-zA-Z]+[a-zA-Z0-9]*)$");
							Matcher m = p.matcher(versionParam);
							if(!m.find())
							{
								System.err.println("Set Feature Version: expecting major.minor.patch-feature (feature must start with alpha)[" + commandLine + "]");
								return EK9.BAD_COMMANDLINE_EXIT_CODE;
							}
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

		if(isHelp())
		{
			System.err.println("ek9 <options>");
			System.err.println(CommandLineDetails.getCommandLineHelp());
			//i.e. no further commands need to run
			return EK9.SUCCESS_EXIT_CODE;
		}
		if(isVersionOfEK9Option())
		{
			System.err.println("EK9 Version 0.0.1-0");
			//i.e. no further commands need to run
			return EK9.SUCCESS_EXIT_CODE;
		}

		//Add in run mode if no options supplied as default.
		if(!isJustABuildTypeOption()
				&& !isReleaseVectorOption()
				&& !isDeveloperManagementOption()
				&& !isRunDebugMode()
				&& !isRunEK9AsLanguageServer())
			ek9AppParameters.add("-r");

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
			{
				//Do nothing in here, external caller will need to check if there is a program to run.
			}
		}
		//i.e. a command does need to run.
		return EK9.RUN_COMMAND_EXIT_CODE;
	}

	public Integer processEK9FileProperties(boolean forceRegeneration)
	{
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

			//TODO additional processing
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
			if (!new JustParser().readSourceFile(mainSourceFile, visitor))
			{
				System.err.println("Unable to Parse source file [" + mainSourceFile.getAbsolutePath() + "]");
				System.exit(EK9.FILE_ISSUE_EXIT_CODE);
			}
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
