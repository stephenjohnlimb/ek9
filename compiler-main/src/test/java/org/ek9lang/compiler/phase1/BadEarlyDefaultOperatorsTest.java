package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad operators when used with default.
 */
class BadEarlyDefaultOperatorsTest extends BadSymbolDefinitionTest {

  public BadEarlyDefaultOperatorsTest() {
    super("/examples/parseButFailCompile/badDefaultUse",
        List.of("earlybad.defaultoperators.examples"));
  }
}
