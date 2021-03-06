package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.cli.support.Reporter;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;

import java.io.File;

/**
 * Abstract base for the command line ek9 commands.
 */
public abstract class E extends Reporter
{
	protected final CommandLineDetails commandLine;
	protected final FileCache sourceFileCache;

	private boolean debuggingInstrumentation = false;
	private boolean devBuild = false;

	public E(CommandLineDetails commandLine, FileCache sourceFileCache)
	{
		super(commandLine.isVerbose());
		this.commandLine = commandLine;
		this.sourceFileCache = sourceFileCache;
	}

	/**
	 * Provide the report/log message prefix
	 */
	protected abstract String messagePrefix();

	protected FileHandling getFileHandling()
	{
		return commandLine.getFileHandling();
	}

	protected OsSupport getOsSupport()
	{
		return commandLine.getOsSupport();
	}

	public boolean run()
	{
		return preConditionCheck() && doRun() && postConditionCheck();
	}

	/**
	 * Do a precondition check to ensure run can execute.
	 *
	 * @return true if all Ok false if precondition not met.
	 */
	public boolean preConditionCheck()
	{
		log("Prepare");
		//Ensure the .ek9 directory exists in users home directory.
		getFileHandling().validateHomeEK9Directory(commandLine.targetArchitecture);
		return true;
	}

	public boolean postConditionCheck()
	{
		log("Complete");
		return true;
	}

	/**
	 * Actually run the execution.
	 */
	protected abstract boolean doRun();

	protected void log(Object message)
	{
		if(commandLine.isVerbose())
			System.err.println(messagePrefix() + message);
	}

	protected void report(Object message)
	{
		System.err.println(messagePrefix() + message);
	}

	public boolean isDebuggingInstrumentation()
	{
		return debuggingInstrumentation || commandLine.isDebuggingInstrumentation();
	}

	public void setDebuggingInstrumentation(boolean debuggingInstrumentation)
	{
		this.debuggingInstrumentation = debuggingInstrumentation;
	}

	public boolean isDevBuild()
	{
		return devBuild || commandLine.isDevBuild();
	}

	public void setDevBuild(boolean devBuild)
	{
		this.devBuild = devBuild;
		this.sourceFileCache.setDevBuild(devBuild);
	}

	protected String getDotEK9Directory()
	{
		return getFileHandling().getDotEK9Directory(commandLine.getSourceFileDirectory());
	}

	protected File getMainGeneratedOutputDirectory()
	{
		return getFileHandling().getMainGeneratedOutputDirectory(getDotEK9Directory(), commandLine.targetArchitecture);
	}

	protected File getMainFinalOutputDirectory()
	{
		return getFileHandling().getMainFinalOutputDirectory(getDotEK9Directory(), commandLine.targetArchitecture);
	}

	protected File getDevGeneratedOutputDirectory()
	{
		return getFileHandling().getDevGeneratedOutputDirectory(getDotEK9Directory(), commandLine.targetArchitecture);
	}

	protected File getDevFinalOutputDirectory()
	{
		return getFileHandling().getDevFinalOutputDirectory(getDotEK9Directory(), commandLine.targetArchitecture);
	}
}
