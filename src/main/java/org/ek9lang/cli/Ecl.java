package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.OsSupport;

/**
 * Clean a project down.
 */
public class Ecl extends E
{
	public Ecl(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "Clean   : ";
	}

	public boolean run()
	{
		log("Prepare");

		getFileHandling().cleanEK9DirectoryStructureFor(commandLine.getFullPathToSourceFileName(), commandLine.targetArchitecture);
		//Now trigger file structure and property file regeneration.
		log("- Props");
		commandLine.processEK9FileProperties(true);

		log("Complete");
		return true;
	}
}
