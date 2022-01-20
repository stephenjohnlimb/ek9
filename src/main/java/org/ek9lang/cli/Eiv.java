package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.OsSupport;

public class Eiv extends Eve
{
	public Eiv(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}

	public boolean run()
	{
		log("Version+: Prepare");
		if(commandLine.isPackagePresent())
		{
			//Need to get from command line.
			String partToIncrement = commandLine.getOptionParameter("-IV");
			Version versionNumber = new Version(commandLine.getVersion());
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
		log("Version+: Complete");

		return true;
	}
}
