package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

/**
 * Run all unit tests inside a project.
 */
public class Et extends E
{
	public Et(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "Test    : ";
	}

	protected boolean doRun()
	{
		log("Compile!");

		//trigger rebuild if needed
		Eic eic = new Eic(commandLine, sourceFileCache);
		eic.setDebuggingInstrumentation(true);
		eic.setDevBuild(true);
		if(eic.run())
		{
			//TODO - call to run all the tests
			return true;
		}
		return false;
	}
}
