package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad use of guards in expressions.
 */
class BadGuardsTest extends BadSymbolDefinitionTest {

  public BadGuardsTest() {
    super("/examples/parseButFailCompile/badGuardsWithExpressions",
        List.of("bad.guards"));
  }
}
