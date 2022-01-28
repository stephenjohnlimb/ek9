package org.ek9lang.core.exception;

public class ExitException extends RuntimeException
{
	private final int exitCode;

	public ExitException(int exitCode, String message)
	{
		super(message);
		this.exitCode = exitCode;
	}

	public ExitException(int exitCode, Throwable throwable)
	{
		super(throwable);
		this.exitCode = exitCode;
	}

	public int getExitCode()
	{
		return exitCode;
	}

}
