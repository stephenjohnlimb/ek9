package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad inheritance usage.
 */
class BadOpenAbstractTraitsTest extends BadSymbolDefinitionTest {

  public BadOpenAbstractTraitsTest() {
    super("/examples/parseButFailCompile/phase1/badOpenAbstractTraits",
        List.of("marked.open.but.stillabstract"));
  }
}
