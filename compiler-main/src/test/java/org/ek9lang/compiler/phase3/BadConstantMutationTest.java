package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Checks for attempts to mutate constants and enumerations.
 */
class BadConstantMutationTest extends BadFullResolutionTest {

  public BadConstantMutationTest() {
    super("/examples/parseButFailCompile/phase3/badConstantUse",
        List.of("bad.mutations"), false, true);
  }

}
