package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad enumerations.
 */
class BadEnumerationsTest extends BadSymbolDefinitionTest {

  public BadEnumerationsTest() {
    super("/examples/parseButFailCompile/badEnumerations",
        List.of("bad.enumerations.check"));
  }
}
