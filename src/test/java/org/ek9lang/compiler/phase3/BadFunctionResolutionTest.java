package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad function resolution, missing parameters etc.
 */
class BadFunctionResolutionTest extends BadFullResolutionTest {

  public BadFunctionResolutionTest() {
    super("/examples/parseButFailCompile/badFunctionResolution",
        List.of("bad.functions.resolution", "auto.function.checks"));
  }

}
