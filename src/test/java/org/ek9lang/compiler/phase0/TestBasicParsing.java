package org.ek9lang.compiler.phase0;

/**
 * Just a simple parse of the Hello World EK9 Source code.
 */
final class TestBasicParsing extends ParsingBase {
  @Override
  protected String getEK9FileName() {
    return "/examples/basics/HelloWorld.ek9";
  }
}
