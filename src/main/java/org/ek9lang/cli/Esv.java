package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

/**
 * Used to explicitly set the version of a package.
 */
public class Esv extends Eve
{
	public Esv(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "Version=: ";
	}

	protected boolean doRun()
	{
		String newVersionParameter = commandLine.getOptionParameter("-SV");
		Version newVersion = Version.withNoBuildNumber(newVersionParameter);
		if(!super.setVersionNewNumber(newVersion))
			return false;

		log("Complete");
		return true;
	}
}
