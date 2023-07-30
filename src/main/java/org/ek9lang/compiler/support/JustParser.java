package org.ek9lang.compiler.support;

import java.io.File;
import java.io.FileInputStream;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.antlr.EK9Parser.CompilationUnitContext;
import org.ek9lang.cli.Ek9SourceVisitor;
import org.ek9lang.compiler.Source;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.tokenizer.ParserCreator;
import org.ek9lang.compiler.tokenizer.ParserSpec;
import org.ek9lang.core.ExceptionConverter;
import org.ek9lang.core.Logger;
import org.ek9lang.core.OsSupport;
import org.ek9lang.core.Processor;

/**
 * For when we just need to read ek9 files and pull out bits and bobs
 * rather than actually compiling.
 */
public class JustParser {
  private final OsSupport osSupport = new OsSupport();

  private final ParserCreator parserCreator = new ParserCreator();

  /**
   * Read the source file using the visitor supplied.
   */
  public boolean readSourceFile(final File sourceFile, final Ek9SourceVisitor visitor) {

    Processor<Boolean> processor = () -> {
      Source src = sourceFile::getName;

      if (!osSupport.isFileReadable(sourceFile)) {
        Logger.error("File [" + sourceFile.getName() + "] is not readable");
        return false;
      }

      try (var inputStream = new FileInputStream(sourceFile)) {
        ErrorListener errorListener = new ErrorListener(src.getFileName());
        var spec = new ParserSpec(src, inputStream, errorListener, null);
        EK9Parser parser = parserCreator.apply(spec);

        CompilationUnitContext context = parser.compilationUnit();

        if (isErrorFree(errorListener)) {
          visitor.visit(context, errorListener);
          return isErrorFree(errorListener);
        }
      }
      return false;
    };
    return new ExceptionConverter<Boolean>().apply(processor);
  }

  private boolean isErrorFree(final ErrorListener errorListener) {
    if (errorListener.hasErrors()) {
      errorListener.getErrors().forEachRemaining(Logger::error);
      return false;
    }
    return true;
  }
}
