package org.ek9lang.compiler.phase0;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.tokenizer.LexerPlugin;
import org.ek9lang.compiler.tokenizer.LexingBase;
import org.ek9lang.core.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Now move beyond lexing, to parsing content.
 */
abstract class ParsingBase extends LexingBase {

  private final ErrorListener errorListener = new ErrorListener("test");
  private EK9Parser underTest;

  @BeforeEach
  void loadTokenStreamInParser() {
    InputStream inputStream = getClass().getResourceAsStream(getEK9FileName());
    assertNotNull(inputStream, "Read File");

    LexerPlugin lexer = getEK9Lexer(inputStream);
    lexer.removeErrorListeners();
    lexer.addErrorListener(errorListener);

    underTest = new EK9Parser(new CommonTokenStream(lexer));
    underTest.removeErrorListeners();
    underTest.addErrorListener(errorListener);
  }

  @Test
  void test() {
    assertNotNull(underTest);

    long before = System.currentTimeMillis();
    EK9Parser.CompilationUnitContext context = underTest.compilationUnit();
    long after = System.currentTimeMillis();

    if (!errorListener.isErrorFree()) {
      Logger.log("Parsing " + (after - before) + "ms for " + getEK9FileName());
      errorListener.getErrors().forEachRemaining(System.out::println);
    }

    assertTrue(errorListener.isErrorFree(), "Parsing of " + getEK9FileName() + " failed");
    assertNotNull(context);
  }
}
