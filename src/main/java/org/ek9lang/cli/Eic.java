package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.OsSupport;

/**
 * Just does an incremental build if possible.
 */
public class Eic extends Ec
{
	public Eic(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		super(commandLine, sourceFileCache, osSupport);
	}

	@Override
	protected String messagePrefix()
	{
		return "Compile: ";
	}

	public boolean run()
	{
		//Check if it even exists - if not then that is a full build that is needed.
		if(!sourceFileCache.isTargetExecutableArtefactPresent())
		{
			log(messagePrefix() + "Missing target  - Compile!");
			Efc execution = new Efc(commandLine, sourceFileCache, osSupport);
			//may have been forced in and so we must pass on.
			execution.setDebuggingInstrumentation(this.isDebuggingInstrumentation());
			execution.setDevBuild((this.isDevBuild()));
			return execution.run();
		}
		else
		{
			//Yes we can do an incremental build.

			if(sourceFileCache.isTargetExecutableArtefactCurrent())
			{
				report(messagePrefix() + "Target already in date");
				return true;
			}
			else
			{
				log(messagePrefix() + "Prepare");
				prepareCompilation();

				if(!compile(sourceFileCache.getIncrementalCompilableProjectFiles()))
				{
					report(messagePrefix() + "failed");
					return false;
				}

				int changesToPackage = sourceFileCache.getIncrementalFilesPartOfBuild().size();
				log(messagePrefix() + changesToPackage + " changed file(s)");

				return repackageTargetArtefact();
			}
		}
	}
}
