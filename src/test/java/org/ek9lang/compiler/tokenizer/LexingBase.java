package org.ek9lang.compiler.tokenizer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.antlr.v4.runtime.CharStreams;
import org.ek9lang.compiler.errors.ErrorListener;

import java.io.InputStream;

public abstract class LexingBase
{
	private ErrorListener errorListener = new ErrorListener();
	
	protected abstract String getEK9FileName();
	
	protected abstract LexerPlugin getEK9Lexer(org.antlr.v4.runtime.CharStream charStream);
	
	@Test
	public void justLex() throws Exception
	{
		InputStream inputStream = getClass().getResourceAsStream(getEK9FileName());

		assertNotNull(inputStream);

		LexerPlugin lexer = getEK9Lexer(CharStreams.fromStream(inputStream));
		lexer.removeErrorListeners();						
		lexer.addErrorListener(errorListener);
		
		String readability = new TokenStreamAssessment().assess(lexer, false);
		System.out.println("Readability of " + getEK9FileName() + " is " + readability);
	}
}
