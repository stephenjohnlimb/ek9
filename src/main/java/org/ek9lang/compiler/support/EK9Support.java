package org.ek9lang.compiler.support;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.tokenizer.EK9Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class EK9Support extends AntlrSupport
{
	public static void main(String... args) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		if(args.length != 1)
			System.out.println("Expect a single argument of the ek9 source file to process");
		else
			new EK9Support(args[0]);
	}

	public EK9Support(String inputFileName) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
	{
		super(inputFileName);
	}

	@Override
	protected Lexer getLexer(CharStream input)
	{
		return new EK9Lexer(input, EK9Parser.INDENT, EK9Parser.DEDENT).setPrintTokensAsSupplied(false);
	}

	@Override
	protected String getGrammarName() { return "org.ek9lang.antlr.EK9";};
}
