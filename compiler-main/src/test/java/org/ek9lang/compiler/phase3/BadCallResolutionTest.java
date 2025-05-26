package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad call resolution.
 * This covers a mix of calls to methods, function and function delegates.
 * Clearly this can get quite complex as all three can look the same!
 */
class BadCallResolutionTest extends BadFullResolutionTest {

  public BadCallResolutionTest() {
    super("/examples/parseButFailCompile/badCallResolution",
        List.of("bad.simple.resolution", "bad.detailed.resolution"));
  }

}
