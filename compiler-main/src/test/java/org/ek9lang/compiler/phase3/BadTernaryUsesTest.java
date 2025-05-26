package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Tests ternary operations, some good and some bad, detecting errors.
 */
class BadTernaryUsesTest extends BadFullResolutionTest {

  public BadTernaryUsesTest() {
    super("/examples/parseButFailCompile/basicTernary",
        List.of("just.ternary"));
  }

}
