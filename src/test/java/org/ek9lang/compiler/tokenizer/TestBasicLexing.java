package org.ek9lang.compiler.tokenizer;

import java.io.InputStream;

/**
 * Just read a single Hello world EK9 source file and test the lexer.
 */
final class TestBasicLexing extends LexingBase {
  @Override
  protected String getEK9FileName() {
    return "/examples/basics/HelloWorld.ek9";
  }

  @Override
  protected LexerPlugin getEK9Lexer(InputStream inputStream) {
    return new DelegatingLexer(ek9LexerForInput.apply(inputStream));
  }
}
