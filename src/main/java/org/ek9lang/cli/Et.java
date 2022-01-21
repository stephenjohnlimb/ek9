package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.OsSupport;

/**
 * Run all unit tests inside a project.
 */
public class Et extends E
{
	public Et(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}
	
	public boolean run()
	{
		log("Test: Compile!");

		//trigger rebuild if needed
		Eic eic = new Eic(commandLine, sourceFileCache, osSupport);
		eic.setDebuggingInstrumentation(true);
		eic.setDevBuild(true);
		if(eic.run())
		{
			//Now run all tests.
			log("Test: Prepare");
			//TODO - call to run all the tests
			log("Test: Complete");

			return true;
		}
		return false;
	}
}
