package org.ek9lang.compiler.parsing;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.ek9lang.antlr.EK9LexerRules;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.antlr.EK9Parser.CompilationUnitContext;
import org.ek9lang.cli.EK9SourceVisitor;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.tokenizer.EK9Lexer;


/**
 * For when we just need to read ek9 files and pull out bits and bobs
 * rather than actually compiling.
 */
public class JustParser
{
	private EK9Parser parser;
	private ErrorListener errorListener = new ErrorListener();
	
	public boolean readSourceFile(File sourceFile, EK9SourceVisitor visitor)
	{
		visitor.setErrorListener(errorListener);
		try
		{			
			if(!sourceFile.exists())
			{
				System.err.println("File [" + sourceFile.getName() + "] cannot be found");
				return false;
			}
			if(!sourceFile.canRead())
			{
				System.err.println("File [" + sourceFile.getName() + "] is not readable");
				return false;
			}
			if(!sourceFile.isFile())
			{
				System.err.println("File [" + sourceFile.getName() + "] is not a file");
				return false;
			}
			try(InputStream inputStream = new FileInputStream(sourceFile))
			{
				//EK9Lexer lex = new ExperimentalEK9Lexer(CharStreams.fromStream(inputStream));
				EK9Lexer lex = new EK9Lexer(CharStreams.fromStream(inputStream), EK9Parser.INDENT, EK9Parser.DEDENT);
				lex.setSourceName(sourceFile.getName());
				lex.removeErrorListeners();

				parser = new EK9Parser(new CommonTokenStream(lex));
				parser.removeErrorListeners();

				lex.addErrorListener(errorListener);
				parser.addErrorListener(errorListener);

				CompilationUnitContext context = parser.compilationUnit();
				if (!errorListener.isErrorFree())
				{
					errorListener.getErrors().forEachRemaining(error -> {
						System.err.println(error);
					});
					return false;
				}

				visitor.visitCompilationUnit(context);
				if (!errorListener.isErrorFree())
				{
					errorListener.getErrors().forEachRemaining(error -> {
						System.err.println(error);
					});
					return false;
				}
				return true;
			}
		}
		catch(Exception ex)
		{
			System.err.println(ex);
			ex.printStackTrace(System.err);
			return false;
		}		
	}
}
