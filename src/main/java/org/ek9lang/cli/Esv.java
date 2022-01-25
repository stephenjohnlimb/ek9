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

	public boolean run()
	{
		log("Prepare");

		if(commandLine.isPackagePresent())
		{
			String newVersionParameter = commandLine.getOptionParameter("-SV");
			Version newVersion = Version.withNoBuildNumber(newVersionParameter);
			if(!super.setVersionNewNumber(newVersion))
			{
				report("Failed to set version in " + commandLine.getSourceFileName());
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
