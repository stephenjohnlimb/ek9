package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad method/property resolution.
 */
class BadMixedResolutionTest extends BadFullResolutionTest {

  public BadMixedResolutionTest() {
    super("/examples/parseButFailCompile/phase3/badMixedResolution",
        List.of("bad.mixed.resolution", "bad.access.attempts"));
  }

}
