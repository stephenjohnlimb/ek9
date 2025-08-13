package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad usage of various flow control structures.
 */
class BadFlowControlTest extends BadFullResolutionTest {

  public BadFlowControlTest() {
    super("/examples/parseButFailCompile/phase3/badFlowControl",
        List.of("bad.switchtypes1", "bad.switchtypes2", "bad.control.types"));
  }

}
