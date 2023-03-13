package org.ek9lang.compiler.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.antlr.EK9Parser.CompilationUnitContext;
import org.ek9lang.cli.support.Ek9SourceVisitor;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.tokenizer.Ek9Lexer;
import org.ek9lang.compiler.tokenizer.Ek9LexerForInput;
import org.ek9lang.core.utils.Logger;
import org.ek9lang.core.utils.OsSupport;

/**
 * For when we just need to read ek9 files and pull out bits and bobs
 * rather than actually compiling.
 */
public class JustParser {
  private final OsSupport osSupport = new OsSupport();

  private final Ek9LexerForInput ek9LexerForInput = new Ek9LexerForInput();

  /**
   * Read the source file using the visitor supplied.
   */
  public boolean readSourceFile(File sourceFile, Ek9SourceVisitor visitor) {
    try {
      if (!osSupport.isFileReadable(sourceFile)) {
        Logger.error("File [" + sourceFile.getName() + "] is not readable");
        return false;
      }
      try (InputStream inputStream = new FileInputStream(sourceFile)) {
        ErrorListener errorListener = new ErrorListener(sourceFile.getName());

        Ek9Lexer lex = ek9LexerForInput.apply(inputStream);
        lex.setSourceName(sourceFile.getName());
        lex.removeErrorListeners();
        lex.addErrorListener(errorListener);

        EK9Parser parser = new EK9Parser(new CommonTokenStream(lex));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        CompilationUnitContext context = parser.compilationUnit();
        if (errorListener.hasErrors()) {
          errorListener.getErrors().forEachRemaining(Logger::error);
          return false;
        }

        visitor.visit(context, errorListener);
        if (errorListener.hasErrors()) {
          errorListener.getErrors().forEachRemaining(Logger::error);
          return false;
        }
        return true;
      }
    } catch (Exception ex) {
      return false;
    }
  }
}
