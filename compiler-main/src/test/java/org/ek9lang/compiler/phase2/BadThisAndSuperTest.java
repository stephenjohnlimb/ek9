package org.ek9lang.compiler.phase2;

import java.util.List;

/**
 * Just tests bad usage of this and super.
 */
class BadThisAndSuperTest extends BadExplicitTypeDefinitionTest {

  public BadThisAndSuperTest() {
    super("/examples/parseButFailCompile/phase2/badThisAndSuper",
        List.of("bad.functions.thisandsuper", "bad.classes.thisandsuper", "bad.components.thisandsuper"),
        false, true);
  }
}
