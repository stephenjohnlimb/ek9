package org.ek9lang.compiler.parsing;


import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.tokenizer.DelegatingLexer;
import org.ek9lang.compiler.tokenizer.EK9Lexer;
import org.ek9lang.compiler.tokenizer.LexerPlugin;
import org.ek9lang.compiler.tokenizer.LexingBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Now move beyond lexing, to parsing content.
 */
public abstract class ParsingBase extends LexingBase
{
	private EK9Parser underTest;
	private final ErrorListener errorListener = new ErrorListener();

	@BeforeEach
	public void loadTokenStreamInParser() throws Exception
	{
		InputStream inputStream = getClass().getResourceAsStream(getEK9FileName());
		assertNotNull(inputStream, "Read File");

		LexerPlugin lexer = getEK9Lexer(CharStreams.fromStream(inputStream));
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);

		underTest = new EK9Parser(new CommonTokenStream(lexer));
		underTest.removeErrorListeners();
		underTest.addErrorListener(errorListener);
	}

	@Override
	protected LexerPlugin getEK9Lexer(CharStream charStream)
	{
		return new DelegatingLexer(new EK9Lexer(charStream, EK9Parser.INDENT, EK9Parser.DEDENT));
	}

	@Test
	public void test() throws Exception
	{
		assertNotNull(underTest);

		long before = System.currentTimeMillis();
		EK9Parser.CompilationUnitContext context = underTest.compilationUnit();
		long after = System.currentTimeMillis();

		System.out.println("Parsing " + (after - before) + "ms for " + getEK9FileName());

		if(!errorListener.isErrorFree())
			errorListener.getErrors().forEachRemaining(System.out::println);

		assertTrue(errorListener.isErrorFree(), "Parsing of " + getEK9FileName() + " failed");
		assertNotNull(context);
	}
}
