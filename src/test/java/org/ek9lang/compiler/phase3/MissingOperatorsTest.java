package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests for missing operators.
 */
class MissingOperatorsTest extends BadFullResolutionTest {

  public MissingOperatorsTest() {
    super("/examples/parseButFailCompile/missingOperators",
        List.of("bad.defaulted.recordoperators", "bad.defaulted.classoperators",
            "bad.overridden.classoperators", "bad.interpolated.strings"));
  }

}
