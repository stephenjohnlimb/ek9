package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

/**
 * Use to explicitly set a feature name on a package.
 */
public class Esf extends Eve
{
	public Esf(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "Feature=: ";
	}

	protected boolean doRun()
	{
		String newVersionParameter = commandLine.getOptionParameter("-SF");
		Version newVersion = Version.withNoBuildNumber(newVersionParameter);
		if(!super.setVersionNewNumber(newVersion))
			return false;

		log("Complete");
		return true;
	}
}