package org.ek9lang.compiler.tokenizer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.core.utils.Logger;
import org.junit.jupiter.api.Test;

public abstract class LexingBase {

  protected final Ek9LexerForInput ek9LexerForInput = new Ek9LexerForInput();

  protected abstract String getEK9FileName();

  protected abstract LexerPlugin getEK9Lexer(InputStream inputStream);

  @Test
  public void justLex() {
    InputStream inputStream = getClass().getResourceAsStream(getEK9FileName());

    assertNotNull(inputStream);

    LexerPlugin lexer = getEK9Lexer(inputStream);
    lexer.removeErrorListeners();
    ErrorListener errorListener = new ErrorListener(getEK9FileName());

    lexer.addErrorListener(errorListener);

    String readability = new TokenStreamAssessment().assess(lexer, false);
    Logger.log("Readability of " + getEK9FileName() + " is " + readability);
  }
}
