package org.ek9lang.compiler.tokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Just read a single Hello world EK9 source file and test the lexer.
 */
final class TestBasicLexing extends LexingBase {
  @Override
  protected String getEK9FileName() {
    return "/examples/basics/HelloWorld.ek9";
  }

  /**
   * Just check the methods work without exception.
   */
  @Test
  void testDelegationOfLexerMethods() {
    var lexer = getEK9Lexer();

    assertNotNull(lexer.getSourceName());
    assertNotNull(lexer.getInputStream());

    var factory = lexer.getTokenFactory();
    assertNotNull(factory);
    lexer.setTokenFactory(factory);

    assertEquals(1, lexer.getLine());
    assertEquals(0, lexer.getCharPositionInLine());
  }
}
