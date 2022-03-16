package org.ek9lang.compiler.tokenizer;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.TokenSource;

/**
 * Due to way Antlr does is class generation we need to make an interface to decouple to enable debugging and alternatives.
 */
public interface LexerPlugin extends TokenSource
{
	int getIndentToken();

	int getDedentToken();

	String getSymbolicName(int tokenType);

	void removeErrorListeners();

	void addErrorListener(ANTLRErrorListener listener);
}
