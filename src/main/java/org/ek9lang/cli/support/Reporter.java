package org.ek9lang.cli.support;

public abstract class Reporter
{
	private final boolean verbose;

	public Reporter(boolean verbose)
	{
		this.verbose = verbose;
	}

	/**
	 * Provide the report/log message prefix
	 */
	protected abstract String messagePrefix();

	protected void log(Object message)
	{
		if(verbose)
			System.err.println(messagePrefix() + message);
	}

	protected void report(Object message)
	{
		System.err.println(messagePrefix() + message);
	}
}
