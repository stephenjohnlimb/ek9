package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.OsSupport;

/**
 * Clean a project down for full resolution and compile.
 */
public class Ecl extends E
{
	public Ecl(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}

	public boolean run()
	{
		log("Clean: Prepare");

		fileHandling.cleanEK9DirectoryStructureFor(commandLine.getFullPathToSourceFileName(), commandLine.targetArchitecture);
		//Now trigger file structure and property file regeneration.
		commandLine.processEK9FileProperties(true);

		log("Clean: Complete");
		return true;
	}
}
