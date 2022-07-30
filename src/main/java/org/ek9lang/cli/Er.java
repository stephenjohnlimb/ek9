package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.EK9DirectoryStructure;

import java.util.Objects;

/**
 * Run the application that has been built or is already built.
 */
public class Er extends E
{
	public Er(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine, sourceFileCache);
	}

	@Override
	protected String messagePrefix()
	{
		return "Run     : ";
	}

	@Override
	public boolean preConditionCheck()
	{
		return Objects.equals(commandLine.targetArchitecture, EK9DirectoryStructure.JAVA) && super.preConditionCheck();
	}

	protected boolean doRun()
	{
		if(!sourceFileCache.isTargetExecutableArtefactCurrent())
		{
			log("Stale target - Compile");

			//So it needs to be built so let's assume an incremental build - but that might decide on full build.
			if(!new Eic(commandLine, sourceFileCache).run())
				return false;
		}

		log("Execute");

		//OK we can issue run command.
		StringBuilder theRunCommand = new StringBuilder("java");
		if(commandLine.isRunDebugMode())
			theRunCommand.append(" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:").append(commandLine.debugPort);

		String target = sourceFileCache.getTargetExecutableArtefact().getAbsolutePath();
		commandLine.getEk9AppDefines().forEach(define -> theRunCommand.append(" ").append("-D").append(define));
		theRunCommand.append(" -classpath");
		theRunCommand.append(" ").append(target);
		theRunCommand.append(" ").append(commandLine.getModuleName()).append(".").append("_").append(commandLine.ek9ProgramToRun);

		commandLine.getEk9ProgramParameters().forEach(param -> theRunCommand.append(" ").append(param));
		System.out.println(theRunCommand);

		return true;
	}
}
