package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Tests bad text construct language codes.
 */
class BadTextConstructsTest extends BadSymbolDefinitionTest {

  public BadTextConstructsTest() {
    super("/examples/parseButFailCompile/phase1/badTextConstructs",
        List.of("bad.text.language.codes"));
  }
}
