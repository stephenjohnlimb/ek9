package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad usage of for loops.
 */
class BadForLoopsTest extends BadFullResolutionTest {

  public BadForLoopsTest() {
    super("/examples/parseButFailCompile/phase3/badForLoops",
        List.of("bad.forloops.check", "bad.forrangeloops.check"));
  }

}
