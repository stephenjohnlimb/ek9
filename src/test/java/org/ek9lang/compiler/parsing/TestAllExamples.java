package org.ek9lang.compiler.parsing;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.tokenizer.DelegatingLexer;
import org.ek9lang.compiler.tokenizer.EK9Lexer;
import org.ek9lang.compiler.tokenizer.LexerPlugin;
import org.ek9lang.compiler.tokenizer.TokenStreamAssessment;
import org.ek9lang.core.utils.Glob;
import org.ek9lang.core.utils.OsSupport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locates both good examples and checks they parse, but also badExamples to check they don't.
 */
final class TestAllExamples
{

	/**
	 * Function just to convert File to file name, ready for output.
	 */
	private final Function<File, String> fileToFileName = File::getName;

	/**
	 * Assesses the readability of a file and returns the result of that readability.
	 */
	private final Function<File, String> readabilityAssessor = ek9SourceFile -> {
		try(var is = new FileInputStream(ek9SourceFile))
		{
			ErrorListener errorListener = new ErrorListener();
			LexerPlugin lexer = getEK9Lexer(CharStreams.fromStream(is));
			lexer.removeErrorListeners();
			lexer.addErrorListener(errorListener);

			String readability = new TokenStreamAssessment().assess(lexer, false);
			return "Readability of " + ek9SourceFile.getName() + " is " + readability;
		}
		catch(Exception ex)
		{
			throw new RuntimeException((ex));
		}
	};

	@Test
	void testValidEK9ExampleSource()
	{
		var func = readabilityAssessor.compose(getTestFunction(false));
		processEK9SourceFilesExpecting("/examples", func);
	}

	@Test
	void testInvalidEK9ExampleSource()
	{
		var func = fileToFileName.compose(getTestFunction(true));
		processEK9SourceFilesExpecting("/badExamples", func);
	}

	private void processEK9SourceFilesExpecting(final String fromDirectory, final Function<File, String> func)
	{
		OsSupport os = new OsSupport();
		URL rootDirectoryForTest = this.getClass().getResource(fromDirectory);
		assertNotNull(rootDirectoryForTest);
		File examples = new File(rootDirectoryForTest.getPath());
		Glob ek9 = new Glob("**.ek9");

		os.getFilesRecursivelyFrom(examples, ek9)
				.parallelStream()
				.map(func)
				.forEach(System.out::println);
	}

	private Function<File, File> getTestFunction(final boolean expectError)
	{
		return ek9SourceFile -> {
			try(var is = new FileInputStream(ek9SourceFile))
			{
				ErrorListener errorListener = new ErrorListener();
				LexerPlugin lexer = getEK9Lexer(CharStreams.fromStream(is));
				lexer.removeErrorListeners();
				lexer.addErrorListener(errorListener);

				EK9Parser parser = new EK9Parser(new CommonTokenStream(lexer));
				parser.removeErrorListeners();
				parser.addErrorListener(errorListener);

				long before = System.currentTimeMillis();
				EK9Parser.CompilationUnitContext context = parser.compilationUnit();
				long after = System.currentTimeMillis();

				System.out.println("Parsed " + ek9SourceFile.getName() + " in " + (after - before) + "ms. Expecting Error [" + expectError + "]");

				if(!expectError)
				{
					if(!errorListener.isErrorFree())
						errorListener.getErrors().forEachRemaining(System.out::println);
					assertTrue(errorListener.isErrorFree(), "Parsing of " + ek9SourceFile.getName() + " failed");
					assertNotNull(context);
				}
				else
				{
					assertFalse(errorListener.isErrorFree(), "Parsing of " + ek9SourceFile.getName() + " should have failed");
				}
				return ek9SourceFile;
			}
			catch(Exception ex)
			{
				throw new RuntimeException(ex);
			}
		};
	}

	private LexerPlugin getEK9Lexer(CharStream charStream)
	{
		return new DelegatingLexer(new EK9Lexer(charStream, EK9Parser.INDENT, EK9Parser.DEDENT));
	}
}
