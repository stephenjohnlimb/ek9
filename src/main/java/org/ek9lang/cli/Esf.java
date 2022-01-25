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

	public boolean run()
	{
		log("Prepare");
		if(commandLine.isPackagePresent())
		{
			String newVersionParameter = commandLine.getOptionParameter("-SF");
			Version newVersion = Version.withNoBuildNumber(newVersionParameter);
			if(!super.setVersionNewNumber(newVersion))
			{
				report("Failed to set version in " + commandLine.getFullPathToSourceFileName());
				return false;
			}
		}
		else
		{
			report("File " + super.commandLine.getSourceFileName() + " does not define a package");
			return false;
		}
		log("Complete");

		return true;
	}
}