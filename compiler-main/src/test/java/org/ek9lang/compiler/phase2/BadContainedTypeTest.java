package org.ek9lang.compiler.phase2;

import java.util.List;

/**
 * Just tests bad constrained type use.
 */
class BadContainedTypeTest extends BadExplicitTypeDefinitionTest {

  public BadContainedTypeTest() {
    super("/examples/parseButFailCompile/phase2/badConstrainedTypes",
        List.of("bad.constrainedtype.examples1", "bad.constrainedtype.examples2"));
  }
}
