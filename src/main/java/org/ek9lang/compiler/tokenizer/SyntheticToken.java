package org.ek9lang.compiler.tokenizer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;

public class SyntheticToken implements Token
{
	@Override
	public String getText()
	{
		return "Synthetic";
	}

	@Override
	public int getType()
	{
		return 0;
	}

	@Override
	public int getLine()
	{
		return 0;
	}

	@Override
	public int getCharPositionInLine()
	{
		return 0;
	}

	@Override
	public int getChannel()
	{
		return 0;
	}

	@Override
	public int getTokenIndex()
	{
		return 0;
	}

	@Override
	public int getStartIndex()
	{
		return 0;
	}

	@Override
	public int getStopIndex()
	{
		return 0;
	}

	@Override
	public TokenSource getTokenSource()
	{
		return new TokenSource()
		{

			@Override
			public Token nextToken()
			{
				return null;
			}

			@Override
			public int getLine()
			{
				return 0;
			}

			@Override
			public int getCharPositionInLine()
			{
				return 0;
			}

			@Override
			public CharStream getInputStream()
			{
				return null;
			}

			@Override
			public String getSourceName()
			{
				return "SyntheticTokenSource";
			}

			@Override
			public void setTokenFactory(TokenFactory<?> factory)
			{

			}

			@Override
			public TokenFactory<?> getTokenFactory()
			{
				return null;
			}
		};
	}

	@Override
	public CharStream getInputStream()
	{
		return null;
	}
}
