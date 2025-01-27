package org.ek9lang.compiler.phase2;

import java.util.List;

/**
 * Just tests bad of components and applications.
 */
class BadApplicationsAndComponentTest extends BadExplicitTypeDefinitionTest {


  public BadApplicationsAndComponentTest() {
    super("/examples/parseButFailCompile/badApplicationsAndComponents",
        List.of("bad.components.use"));
  }
}
