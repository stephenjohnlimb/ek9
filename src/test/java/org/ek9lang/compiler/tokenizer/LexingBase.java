package org.ek9lang.compiler.tokenizer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.core.utils.Logger;
import org.junit.jupiter.api.Test;

public abstract class LexingBase {

  protected final Ek9LexerForInput ek9LexerForInput = new Ek9LexerForInput();

  protected abstract String getEK9FileName();


  protected DelegatingLexer getEK9Lexer(InputStream inputStream) {
    return new DelegatingLexer(ek9LexerForInput.apply(inputStream));
  }


  protected DelegatingLexer getEK9Lexer() {
    var fileName = getEK9FileName();
    ErrorListener errorListener = new ErrorListener(fileName);
    InputStream inputStream = getClass().getResourceAsStream(fileName);
    assertNotNull(inputStream);
    var lexer = getEK9Lexer(inputStream);
    lexer.removeErrorListeners();
    lexer.addErrorListener(errorListener);

    return lexer;
  }

  @Test
  public void justLex() {

    var lexer = getEK9Lexer();

    String readability = new TokenStreamAssessment().assess(lexer, false);
    Logger.log("Readability of " + getEK9FileName() + " is " + readability);
  }
}
