package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests generic classes with constraints.
 */
class BadGenericConstraintsTest extends BadFullResolutionTest {

  public BadGenericConstraintsTest() {
    super("/examples/parseButFailCompile/phase3/badGenericConstraints",
        List.of("bad.generic.class.constraints",
            "bad.generic.class.function.constraints",
            "functiondelegate.inrecord.withgeneric"));
  }

}
