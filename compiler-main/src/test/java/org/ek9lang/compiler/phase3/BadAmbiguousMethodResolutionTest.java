package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad usage of various flow control structures.
 */
class BadAmbiguousMethodResolutionTest extends BadFullResolutionTest {

  public BadAmbiguousMethodResolutionTest() {
    super("/examples/parseButFailCompile/phase3/ambiguousMethods",
        List.of("ambiguous.methods.one", "ambiguous.methods.two"), false, true);
  }

}
