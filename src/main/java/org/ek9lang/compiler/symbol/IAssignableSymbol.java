package org.ek9lang.compiler.symbol;

import org.antlr.v4.runtime.Token;

public interface IAssignableSymbol
{
	default boolean isInitialised()
	{
		return getInitialisedBy() != null;
	}

	Token getInitialisedBy();

	void setInitialisedBy(Token initialisedBy);
}
