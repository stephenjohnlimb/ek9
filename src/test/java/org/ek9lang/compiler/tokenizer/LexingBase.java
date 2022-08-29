package org.ek9lang.compiler.tokenizer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import org.antlr.v4.runtime.CharStreams;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.core.utils.Logger;
import org.junit.jupiter.api.Test;

public abstract class LexingBase {

  protected abstract String getEK9FileName();

  protected abstract LexerPlugin getEK9Lexer(org.antlr.v4.runtime.CharStream charStream);

  @Test
  public void justLex() throws Exception {
    InputStream inputStream = getClass().getResourceAsStream(getEK9FileName());

    assertNotNull(inputStream);

    LexerPlugin lexer = getEK9Lexer(CharStreams.fromStream(inputStream));
    lexer.removeErrorListeners();
    ErrorListener errorListener = new ErrorListener(getEK9FileName());

    lexer.addErrorListener(errorListener);

    String readability = new TokenStreamAssessment().assess(lexer, false);
    Logger.log("Readability of " + getEK9FileName() + " is " + readability);
  }
}
