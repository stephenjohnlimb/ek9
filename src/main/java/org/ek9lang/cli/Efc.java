package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.OsSupport;

/**
 * Do a full recompile of all sources inside project.
 * Not this triggers a full resolution of dependencies and those too are pulled again
 * and recompiled.
 */
public class Efc extends Ec
{
	public Efc(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}

	@Override
	protected String messagePrefix()
	{
		return "Compile!: ";
	}

	public boolean run()
	{
		log(messagePrefix() + "Prepare - Resolve");
		Edp edp = new Edp(commandLine, sourceFileCache, osSupport);
		if(edp.run())
		{
			log(messagePrefix() + "Prepare");
			prepareCompilation();

			if(!compile(sourceFileCache.getAllCompilableProjectFiles()))
			{
				report(messagePrefix() + "failed");
				return false;
			}

			return repackageTargetArtefact();
		}
		return false;
	}
}
