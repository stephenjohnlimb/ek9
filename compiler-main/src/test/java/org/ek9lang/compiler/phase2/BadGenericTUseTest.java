package org.ek9lang.compiler.phase2;

import java.util.List;

/**
 * Just tests generics and detection of T and other conceptual types.
 */
class BadGenericTUseTest extends BadExplicitTypeDefinitionTest {

  public BadGenericTUseTest() {
    super("/examples/parseButFailCompile/phase2/badGenericTUse",
        List.of("bad.use.conceptual.parameters", "bad.generic.constraining.types"));
  }
}
