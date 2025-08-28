package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad flow controls.
 */
class BadFlowControlTest extends BadSymbolDefinitionTest {

  public BadFlowControlTest() {
    super("/examples/parseButFailCompile/phase1/badFlowControl",
        List.of("bad.switches", "bad.ifs"));
  }
}
