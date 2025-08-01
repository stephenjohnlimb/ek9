package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Test generic type parameterization and subsequent calls.
 * Shows different syntax to parameterize a generic type and also some that are incorrect.
 */
class GenericParameterizationAndCallsTest extends BadFullResolutionTest {

  public GenericParameterizationAndCallsTest() {
    super("/examples/parseButFailCompile/genericParameterizationAndCalls",
        List.of("generic.parameterization"), false, true);
  }

}
