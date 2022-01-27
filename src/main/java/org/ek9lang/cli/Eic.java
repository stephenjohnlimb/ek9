package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;

/**
 * Just does an incremental build if possible.
 */
public class Eic extends Ec
{
	public Eic(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "Compile : ";
	}

	protected boolean doRun()
	{
		//Check if it even exists - if not then that is a full build that is needed.
		if(!sourceFileCache.isTargetExecutableArtefactPresent())
		{
			log("Missing target - Compile!");
			Efc execution = new Efc(commandLine, sourceFileCache);
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
				log("Target already in date");
				return true;
			}
			else
			{
				log("Prepare");
				prepareCompilation();

				if(!compile(sourceFileCache.getIncrementalCompilableProjectFiles()))
				{
					report("Failed");
					return false;
				}

				int changesToPackage = sourceFileCache.getIncrementalFilesPartOfBuild().size();
				log(changesToPackage + " changed file(s)");

				return repackageTargetArtefact();
			}
		}
	}
}
