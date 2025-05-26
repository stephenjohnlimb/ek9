package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Checks for viable/missing operators when defining constrained types.
 */
class BadOperatorsOnConstrainedTypesTest extends BadFullResolutionTest {

  public BadOperatorsOnConstrainedTypesTest() {
    super("/examples/parseButFailCompile/badConstrainedOperators",
        List.of("bad.constrainedtypeoperators.examples1"));
  }

}
