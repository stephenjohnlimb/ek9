package org.ek9lang.compiler.tokenizer;

import org.antlr.v4.runtime.CharStream;
import org.ek9lang.antlr.EK9Parser;

/**
 * Just read a single Hello world EK9 source file and test the lexer.
 */
final class TestBasicLexing extends LexingBase {
  @Override
  protected String getEK9FileName() {
    return "/examples/basics/HelloWorld.ek9";
  }

  @Override
  protected LexerPlugin getEK9Lexer(CharStream charStream) {
    return new DelegatingLexer(new Ek9Lexer(charStream, EK9Parser.INDENT, EK9Parser.DEDENT));
  }
}
