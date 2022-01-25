package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

/**
 * Just increments a version number.
 */
public class Eiv extends Eve
{
	public Eiv(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "Version+: ";
	}

	public boolean run()
	{
		log("Prepare");
		if(commandLine.isPackagePresent())
		{
			//Need to get from command line.
			String partToIncrement = commandLine.getOptionParameter("-IV");
			Version versionNumber = Version.withBuildNumber(commandLine.getVersion());
			switch(partToIncrement)
			{
				case "major":
					versionNumber.incrementMajor();
					break;
				case "minor":
					versionNumber.incrementMinor();
					break;
				case "patch":
					versionNumber.incrementPatch();
					break;
				case "build":
					versionNumber.incrementBuildNumber();
					break;
			}
			if(!super.setVersionNewNumber(versionNumber))
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
