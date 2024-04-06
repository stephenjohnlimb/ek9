package org.ek9lang.compiler.common;

import java.io.File;
import java.io.FileInputStream;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.Source;
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
  private final boolean reportErrors;
  private final OsSupport osSupport = new OsSupport();
  private final ParserCreator parserCreator = new ParserCreator();

  /**
   * Create new Parser.
   */
  public JustParser(final boolean reportErrors) {

    this.reportErrors = reportErrors;

  }

  /**
   * Read the source file using the visitor supplied.
   */
  public boolean readSourceFile(final File sourceFile, final Ek9SourceVisitor visitor) {

    Processor<Boolean> processor = () -> {
      final Source src = sourceFile::getName;

      if (!osSupport.isFileReadable(sourceFile)) {
        return false;
      }

      try (var inputStream = new FileInputStream(sourceFile)) {
        final var errorListener = new ErrorListener(src.getFileName());
        final var spec = new ParserSpec(src, inputStream, errorListener, null);

        EK9Parser parser = parserCreator.apply(spec);

        final var context = parser.compilationUnit();
        final var parseIsErrorFree = isErrorFree(errorListener);

        if (parseIsErrorFree) {
          visitor.visit(context, errorListener);
          return isErrorFree(errorListener);
        }
      }

      return false;
    };

    return new ExceptionConverter<Boolean>().apply(processor);
  }

  private boolean isErrorFree(final ErrorListener errorListener) {

    final var errorFree = errorListener.isErrorFree();
    if (!errorFree && reportErrors) {
      errorListener.getErrors().forEachRemaining(Logger::error);
    }
    return errorFree;
  }
}
