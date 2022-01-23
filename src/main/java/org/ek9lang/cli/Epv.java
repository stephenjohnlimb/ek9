package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.OsSupport;

/**
 * Print the version number of the package.
 */
public class Epv extends E
{
	public Epv(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}

	@Override
	protected String messagePrefix()
	{
		return "$Version: ";
	}

	public boolean run()
	{
		log("Prepare");

		if(commandLine.isPackagePresent())
		{
			report(commandLine.getVersion());
		}
		else
		{
			report("File " + commandLine.getSourceFileName() + " does not define a package");
			return false;
		}
		log("Complete");

		return true;
	}
}
