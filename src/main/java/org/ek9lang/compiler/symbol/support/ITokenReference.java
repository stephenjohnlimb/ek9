package org.ek9lang.compiler.symbol.support;

import org.antlr.v4.runtime.Token;

public interface ITokenReference
{
	Token getSourceToken();

	void setSourceToken(Token sourceToken);
}
