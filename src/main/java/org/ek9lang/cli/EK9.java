package org.ek9lang.cli;

import org.ek9lang.lsp.Server;


/**
 * Main entry point into the compiler, build system and language server.
 * Will probably be wrapped in a native C executable at some point - or even moved native.
 * But for now just use java -jar ek9.jar
 */
public class EK9
{
	/**
	 * java -jar ek9.jar -C ./some/path/to/file.ek9
	 *
	 * From an end user point of view they would interact with the script so they would only write:
	 * ./some/path/to/file.ek9 -d 2 -f someother.txt
	 * or ek9 ./some/path/to/file.ek9 -d 2 -f someother.txt
	 * etc
	 * ./some/path/to/file.ek9 -r UDPServer1 -d 2 -f someother.txt
	 *
	 * Note exit code 0 mean there is a command run run printed to stdout.
	 * Exit code 1 means there is no further command to run but what you asked to do worked.
	 * Exit code 2 means there was something wrong with the command line parameters.
	 * Exit code 3 means there was some sort of file processing issue not found, missing content etc.
	 * Exit code 4 means you used an invalid combination of command line parameters or there were inappropriate.
	 * Exit code 5 means the ek9 file you specified does not have any programs in it.
	 * Exit code 6 means the ek9 file does have more than one program and you did not specify which to run.
	 * Exit code 7 means the ek9 compiler when running as LSP failed.
	 * @param argv
	 */
	public static void main(String[] argv)
	{
		if(argv.length == 0)
		{
			System.err.println("ek9 <options>");
			System.err.println(CommandLineDetails.getCommandLineHelp());
			System.exit(2);
		}
		else
		{
			CommandLineDetails commandLine = new CommandLineDetails();
			int result = commandLine.processCommandLine(argv[0]);
			if(result > 0)
				System.exit(result);
			
			//Maybe do this below with hash table of command option to E (execution)!.			
			if(commandLine.isRunEK9AsLanguageServer())
			{
				try
		        {
					System.err.println("EK9 running as LSP languageHelp="+commandLine.isEK9LanguageServerHelpEnabled());
		        	Server.runEK9LanguageServer(System.in, System.out, commandLine.isEK9LanguageServerHelpEnabled());
		        	System.exit(0);
		        }
		        catch(Exception ex)
		        {
		        	System.err.println("Failed to Start Language Server");
		        	System.exit(7);
		        }								
			}
			else if(commandLine.isJustABuildTypeOption())
			{
				/*
				if(commandLine.isCleanAll())
					execution = new Ecl(commandLine, sourceFileCache);
				else if(commandLine.isResolveDependencies())
					execution = new Edp(commandLine, sourceFileCache);
				else if(commandLine.isIncrementalCompile())
					execution = new Eic(commandLine, sourceFileCache);
				else if(commandLine.isFullCompile())
					execution = new Efc(commandLine, sourceFileCache);
				else if(commandLine.isPackaging())
					execution = new Ep(commandLine, sourceFileCache);
				else if(commandLine.isInstall())
					execution = new Ei(commandLine, sourceFileCache);
				else if(commandLine.isDeployment())
					execution = new Ed(commandLine, sourceFileCache);
				execution.run();
				*/
				System.exit(1);
			}
			else if(commandLine.isReleaseVectorOption())
			{
				/*
				if(commandLine.isIncrementReleaseVector())
					execution = new Eiv(commandLine, sourceFileCache);
				else if(commandLine.isSetReleaseVector())
					execution = new Esv(commandLine, sourceFileCache);
				else if(commandLine.isSetFeatureVector())
					execution = new Esf(commandLine, sourceFileCache);
				else if(commandLine.isPrintReleaseVector())
					execution = new Epv(commandLine, sourceFileCache);
				execution.run();
				*/
				System.exit(1);
			}
			else if(commandLine.isDeveloperManagementOption())
			{
				/*
				execution = new Egk(commandLine, sourceFileCache);
				execution.run();
				*/
				System.exit(1);
			}
			else if(commandLine.isUnitTestExecution())
			{
				/*
				execution = new Et(commandLine, sourceFileCache);
				execution.run();
				*/
				System.exit(1);
			}
			else if(commandLine.isRunOption())
			{		
				/*
				execution = new Er(commandLine, sourceFileCache);
				execution.run();
				*/
				System.exit(0);
			}
			else
			{
				System.err.println("Not sure");
				System.exit(2);
			}
		}
	}	
}
