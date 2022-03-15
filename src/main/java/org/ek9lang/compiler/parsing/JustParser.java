package org.ek9lang.compiler.parsing;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.antlr.EK9Parser.CompilationUnitContext;
import org.ek9lang.cli.support.EK9SourceVisitor;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.tokenizer.EK9Lexer;
import org.ek9lang.core.utils.OsSupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


/**
 * For when we just need to read ek9 files and pull out bits and bobs
 * rather than actually compiling.
 */
public class JustParser
{
	private final OsSupport osSupport = new OsSupport();

	private final ErrorListener errorListener = new ErrorListener();

	public boolean readSourceFile(File sourceFile, EK9SourceVisitor visitor)
	{
		visitor.setErrorListener(errorListener);
		try
		{
			if(!osSupport.isFileReadable(sourceFile))
			{
				System.err.println("File [" + sourceFile.getName() + "] os not readable");
				return false;
			}
			try(InputStream inputStream = new FileInputStream(sourceFile))
			{
				//EK9Lexer lex = new ExperimentalEK9Lexer(CharStreams.fromStream(inputStream));
				EK9Lexer lex = new EK9Lexer(CharStreams.fromStream(inputStream), EK9Parser.INDENT, EK9Parser.DEDENT);
				lex.setSourceName(sourceFile.getName());
				lex.removeErrorListeners();

				EK9Parser parser = new EK9Parser(new CommonTokenStream(lex));
				parser.removeErrorListeners();

				lex.addErrorListener(errorListener);
				parser.addErrorListener(errorListener);

				CompilationUnitContext context = parser.compilationUnit();
				if(errorListener.hasErrors())
				{
					errorListener.getErrors().forEachRemaining(System.err::println);
					return false;
				}

				visitor.visitCompilationUnit(context);
				if(errorListener.hasErrors())
				{
					errorListener.getErrors().forEachRemaining(System.err::println);
					return false;
				}
				return true;
			}
		}
		catch(Exception ex)
		{
			return false;
		}
	}
}
