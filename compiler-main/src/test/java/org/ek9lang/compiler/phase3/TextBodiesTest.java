package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Test text constructs.
 */
class TextBodiesTest extends BadFullResolutionTest {

  public TextBodiesTest() {
    super("/examples/parseButFailCompile/phase3/badTextConstructs",
        List.of("bad.missingtextmethods.examples1"));
  }

}
