package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad class modifier usage.
 */
class BadClassModifiersTest extends BadSymbolDefinitionTest {

  public BadClassModifiersTest() {
    super("/examples/parseButFailCompile/phase1/badClassMethods",
        List.of("bad.classmodifier.use"));
  }
}
