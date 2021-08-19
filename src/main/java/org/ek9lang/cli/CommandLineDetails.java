package org.ek9lang.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Just deals with handling the command line options for the compiler.
 *
 */
public class CommandLineDetails
{	
	List<String> ek9AppParameters = new ArrayList<>();
	List<String> ek9AppDefines = new ArrayList<>();
	List<String> ek9ProgramParameters = new ArrayList<>();

	String ek9ProgramToRun = null;	
	String ek9JustFileName = null;	
	String targetArchitecture = "java";
	int debugPort = 8000;
		
	private boolean foundEk9File = false;	
	
	public CommandLineDetails()
	{
	}
	
	/**
	 * Process the command line.
	 * @param commandLine The command line the user or remote system used.
	 * @return error code, 0 no error - see EK9.java for the use of other error codes.
	 */
	public int processCommandLine(String commandLine)
	{
		if(commandLine == null)
		{
			System.err.println("CommandLine is null");
			return 2;
		}
		
		boolean processingEK9Parameters = true;
		List<String> activeParameters = ek9AppParameters;
		//Need to break this up remove extra spaces unless in quotes.
		String[] strArray = commandLine.split(" +(?=([^\']*\'[^\']*\')*[^\']*$)");
		
		for(int i=0; i<strArray.length; i++)
		{				
			if(strArray[i].endsWith("ek9") && !strArray[i].contains("="))
			{
				foundEk9File = true;
				ek9JustFileName = strArray[i];
				processingEK9Parameters = false;
				activeParameters = ek9ProgramParameters;
			}
			else if(strArray[i].equals("-e") && i<strArray.length-1)
			{
				ek9AppDefines.add(strArray[++i].replaceAll("\'", "\""));
			}
			else if(strArray[i].equals("-T") && i<strArray.length-1)
			{
				targetArchitecture = strArray[++i].toLowerCase();
				if(!targetArchitecture.equals("java"))
				{
					System.err.println("Only Java is currently supported as a target [" + commandLine + "]");
					return 2;
				}
			}
			else if(strArray[i].equals("-r") && i<strArray.length-1)
			{
				//So next is program to run
				ek9ProgramToRun = strArray[++i];
			}
			else if(strArray[i].equals("-d") && i<strArray.length-1)
			{				
				if(!acceptableParameter(strArray[i]))
				{
					System.err.println("Incompatible command line options [" + commandLine + "]");
					return 2;
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
					return 2;
				}
				this.debugPort = Integer.parseInt(port);
			}
			else
			{
				if(processingEK9Parameters && !acceptableParameter(strArray[i]))
				{
					System.err.println("Incompatible command line options [" + commandLine + "]");
					return 2;
				}
				activeParameters.add(strArray[i]);
				//Need to consume next option.
				if(strArray[i].equals("-IV") || strArray[i].equals("-SV") || strArray[i].equals("-SF"))
				{
					String versioningOption = strArray[i];
					if(i<strArray.length-1)
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
								return 2;
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
								return 2;
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
								return 2;
							}
						}
						activeParameters.add(versionParam);
					}
					else
					{
						System.err.println("Missing parameter [" + commandLine + "]");
						return 2;
					}
				}
			}								
		}		
		
		if(isHelp())
		{
			System.err.println("ek9 <options>");
			System.err.println(CommandLineDetails.getCommandLineHelp());
			return 0;
		}
		if(isVersionOfEK9Option())
		{
			System.err.println("EK9 Version 0.0.1-0");				
			return 0;
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
			return 4;
		}
		
		if(!isDeveloperManagementOption() && !isRunEK9AsLanguageServer())
		{
			if(!foundEk9File)
			{
				System.err.println("no EK9 file name in command line [" + commandLine + "]");
				return 3;
			}
				
			if(isJustABuildTypeOption() && ek9ProgramToRun != null)
			{
				System.err.println("A Build request for " + ek9JustFileName + " does not require or use program parameters.");
				return 4;
			}
			if(isReleaseVectorOption() && ek9ProgramToRun != null)
			{
				System.err.println("A modification to version number for " + ek9JustFileName + " does not require or use program parameters.");
				return 4;
			}
			
			if(isRunOption())
			{
				//Do nothing in here, external caller will need to check if there is a program to run.
			}
		}
		return 0;
	}	

	public static String getCommandLineHelp()
	{
		StringBuffer buffer = new StringBuffer("where possible options include:\n");
		buffer.append("\t").append("-V The version of the compiler/runtime\n");
		buffer.append("\t").append("-ls Run compiler as Language Server\n");
		buffer.append("\t").append("-lsh Provide EK9 Language Help/Hover\n");
		buffer.append("\t").append("-h Help message\n");
		buffer.append("\t").append("-c Incremental compile; but don't run\n");
		buffer.append("\t").append("-cg Incremental compile but with debugging information; but don't run\n");
		buffer.append("\t").append("-cd Incremental compile but with all dev code and debugging information; but don't run\n");
		buffer.append("\t").append("-C Force full recompilation; but don't run\n");
		buffer.append("\t").append("-Cg Force full recompilation but with debugging information; but don't run\n");
		buffer.append("\t").append("-Cd Force full recompilation but with all dev code and debugging information; but don't run\n");
		buffer.append("\t").append("-Cl Clean all, including dependencies\n");
		buffer.append("\t").append("-Dp Resolve all dependencies, this triggers -Cl clean all first\n");
		buffer.append("\t").append("-P Package ready for deployment to artefact server/library. You decide if software is fit to package.\n");
		buffer.append("\t").append("-I Install packaged software to your local library.\n");
		buffer.append("\t").append("-Gk Generate signing keys for deploying packages to an artefact server\n");
		buffer.append("\t").append("-D Deploy packaged software to an artefact server, this triggers -P packaging first and -Gk if necessary\n");
		buffer.append("\t").append("-IV [major|minor|patch|build] - increment part of the release vector\n");
		buffer.append("\t").append("-SV major.minor.patch - force setting to specific value i.e 6.8.1-0 (always zeros build on setting)\n");
		buffer.append("\t").append("-SF major.minor.patch-feature - force setting to specific value i.e 6.8.0-specials-0 (always zeros build on setting)\n");
		buffer.append("\t").append("-PV print out the current version i.e 6.8.1-0 or 6.8.0-specials-0 for a feature.\n");
		buffer.append("\t").append("-v Verbose mode\n");
		buffer.append("\t").append("-t Runs all unit tests that have been found, this triggers -Cd full compile first\n");
		buffer.append("\t").append("-d port Run in debug mode (requires debugging information - on a port)\n");		
		buffer.append("\t").append("-e <name>=<value> set an environment variable ie. user=Steve or user=\'Steve Limb\' for spaces\n");
		buffer.append("\t").append("-T target architecture - defaults to 'java' if not specified.\n");
		buffer.append("\t").append("filename.ek9 - the main file to work with\n");
		buffer.append("\t").append("-r Program to run (if EK9 file as more than one)\n");
		
		return buffer.toString();
	}		
	
	private boolean acceptableParameter(String param)
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
	
	private boolean isModifierParam(String param)
	{
		switch(param)
		{
			case "-V":
			case "-h":
			case "-v":
			case "-ls":
			case "-lsh":
				return true;	
			default:				
				return false;
		}
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

	public String getTargetArchitecture()
	{
		return targetArchitecture;
	}

	public String getProgramToRun()
	{
		//Might not have been set if there is just one program that is implicit.
		return ek9ProgramToRun;
	}
	
	public String getSourceFileName()
	{
		return ek9JustFileName;
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
