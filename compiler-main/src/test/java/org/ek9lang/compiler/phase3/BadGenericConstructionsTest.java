package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad construction of generics in an assignment.
 */
class BadGenericConstructionsTest extends BadFullResolutionTest {

  public BadGenericConstructionsTest() {
    super("/examples/parseButFailCompile/phase3/inferredConstructionOfGenerics",
        List.of("bad.generic.constructions"));
  }

}
