package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad injection usage.
 */
class BadInjectionsTest extends BadFullResolutionTest {

  public BadInjectionsTest() {
    super("/examples/parseButFailCompile/phase3/badInjections",
        List.of("bad.injection.examples"));
  }

}
