package org.ek9lang.compiler.tokenizer;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.util.regex.Pattern;

/**
 * But also to aid in listing out tokens so that when building grammars out of tokens
 * it is possible to see what is being produced.
 * Note the use of the LexerPlugin, this is so we can alter the underlying Lexer to get the right stream of tokens.
 * <p>
 * As an aside - aids in assessing the readability of source code.
 */
public class TokenStreamAssessment
{
	public String assess(LexerPlugin lexer, boolean printTokens)
	{
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		tokens.fill();

		if(printTokens)
			System.out.println("\n[TOKENS]");

		int numWords = 0;
		int numLetters = 0;
		int numIndents = 0;
		int numNewLines = 0;
		for(Token t : tokens.getTokens())
		{
			String symbolicName = lexer.getSymbolicName(t.getType());
			String literalContent = t.getText().replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");

			String symbolicContent = symbolicName == null ? literalContent : symbolicName;

			if(printTokens)
				System.out.printf("  %-20s '%s'\n", symbolicContent, literalContent);

			if(symbolicContent.equals("NL") || symbolicContent.equals("newline") || symbolicContent.equals("indent") || symbolicContent.equals("dedent"))
			{
				if(symbolicContent.equals("NL"))
					numNewLines++;
				if(symbolicContent.equals("indent"))
					numIndents++;
			}
			else
			{
				if(!Pattern.matches("\\p{Punct}", literalContent) && !literalContent.equals("<-") && !literalContent.equals("->") && !literalContent.equals(":="))
				{
					if(literalContent.startsWith("\""))
					{
						String[] words = literalContent.split("\\s+");
						numWords += words.length;
						for(String word : words)
						{
							numLetters += word.length();
						}
					}
					else
					{
						numLetters += literalContent.length();
						numWords++;
					}
				}
				else
				{
					int contentLength = literalContent.length();
					if(contentLength != 1 && !literalContent.equals("<-") && !literalContent.equals("->") && !literalContent.equals(":="))
					{
						numWords++;
					}
					numLetters += contentLength;
				}
			}
		}

		//We have to approximate sentences in source code
		String readability = new org.ek9lang.core.metrics.ARI().getScore(numLetters, numWords, numIndents, numNewLines);
		if(printTokens)
			System.out.println("Readability [" + readability + "]");
		return readability;
	}
}
