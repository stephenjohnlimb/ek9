package org.ek9lang.compiler.tokenizer;

import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;

import java.util.ArrayList;
import java.util.List;

public class TokenResult
{
	private Token token;
	private List<Token> tokensInLine = new ArrayList<>();
	private int tokenPositionInLine = -1;

	public TokenResult()
	{
	}

	public TokenResult(Token token, List<Token> tokensInLine, int positionInLine)
	{
		this.token = token;
		this.tokenPositionInLine = positionInLine;
		this.tokensInLine = tokensInLine;
	}

	public Token getToken()
	{
		return token;
	}

	public int getTokenPositionInLine()
	{
		return tokenPositionInLine;
	}

	public boolean isPresent()
	{
		return token != null;
	}

	/**
	 * @return true if all previous tokens are indents or this is first token.
	 */
	public boolean previousTokensIndentsOrFirst()
	{
		boolean rtn = true;
		for(int i = 0; i < tokenPositionInLine; i++)
		{
			boolean isIndent = tokensInLine.get(i).getType() == EK9Parser.INDENT;
			rtn &= isIndent;
		}
		return rtn;
	}

	public boolean previousTokenIsAssignment()
	{
		if(tokenPositionInLine == 0)
			return false;

		return tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.ADD_ASSIGN ||
				tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.SUB_ASSIGN ||
				tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.MUL_ASSIGN ||
				tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.DIV_ASSIGN ||
				tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.ASSIGN ||
				tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.ASSIGN2 ||
				tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.COLON ||
				tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.LEFT_ARROW;
	}

	public boolean previousTokenIsPipe()
	{
		if(tokenPositionInLine > 0)
			return tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.PIPE;
		return false;
	}

	public boolean previousTokenIsDefines()
	{
		if(tokenPositionInLine > 0)
			return tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.DEFINES;
		return false;
	}

	public boolean previousTokenIsOverride()
	{
		if(tokenPositionInLine > 0)
			return tokensInLine.get(tokenPositionInLine - 1).getType() == EK9Parser.OVERRIDE;
		return false;
	}
}
