package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

/**
 * Do a full recompile of all sources inside project.
 * Not this triggers a full resolution of dependencies and those too are pulled again
 * and recompiled.
 */
public class Efc extends Ec
{
	public Efc(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "Compile!: ";
	}

	protected boolean doRun()
	{
		Edp edp = new Edp(commandLine, sourceFileCache);
		if(edp.run())
		{
			prepareCompilation();
			return compile(sourceFileCache.getAllCompilableProjectFiles()) && repackageTargetArtefact();
		}
		return false;
	}
}
