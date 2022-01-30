package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

/**
 * Print the version number of the package.
 */
public class Epv extends Eve
{
	public Epv(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "$Version: ";
	}

	protected boolean doRun()
	{
		report(commandLine.getVersion());
		return true;
	}
}
