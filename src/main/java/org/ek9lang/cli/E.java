package org.ek9lang.cli;

import org.ek9lang.cli.support.FileCache;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;

import java.io.File;

/**
 * Abstract base for the command line ek9 commands.
 */
public abstract class E
{
	protected final FileHandling fileHandling;
	protected final CommandLineDetails commandLine;
	protected final OsSupport osSupport;
	protected final FileCache sourceFileCache;

	private boolean debuggingInstrumentation = false;
	private boolean devBuild = false;

	public E(CommandLineDetails commandLine, FileCache sourceFileCache, OsSupport osSupport)
	{
		this.commandLine = commandLine;
		this.sourceFileCache = sourceFileCache;
		this.osSupport = osSupport;
		this.fileHandling = new FileHandling(osSupport);
	}

	/**
	 * Provide the report/log message prefix
	 */
	protected abstract String messagePrefix();

	public abstract boolean run();

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
		return fileHandling.getDotEK9Directory(commandLine.getSourceFileDirectory());
	}

	protected File getMainGeneratedOutputDirectory()
	{
		return fileHandling.getMainGeneratedOutputDirectory(getDotEK9Directory(), commandLine.targetArchitecture);
	}

	protected File getMainFinalOutputDirectory()
	{
		return fileHandling.getMainFinalOutputDirectory(getDotEK9Directory(), commandLine.targetArchitecture);
	}

	protected File getDevGeneratedOutputDirectory()
	{
		return fileHandling.getDevGeneratedOutputDirectory(getDotEK9Directory(), commandLine.targetArchitecture);
	}

	protected File getDevFinalOutputDirectory()
	{
		return fileHandling.getDevFinalOutputDirectory(getDotEK9Directory(), commandLine.targetArchitecture);
	}
}
