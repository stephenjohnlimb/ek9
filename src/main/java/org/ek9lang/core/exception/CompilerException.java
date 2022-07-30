package org.ek9lang.core.exception;

/**
 * Our wrapper for an unchecked exception.
 */
public class CompilerException extends RuntimeException
{
	public CompilerException(String reason)
	{
		super(reason);
	}

	public CompilerException(Throwable cause)
	{
		super(cause);
	}

	public CompilerException(String reason, Throwable cause)
	{
		super(reason, cause);
	}
}
