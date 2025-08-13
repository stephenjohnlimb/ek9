package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad usage of traits.
 */
class BadTraitUseTest extends BadFullResolutionTest {

  public BadTraitUseTest() {
    super("/examples/parseButFailCompile/phase3/badTraitUse",
        List.of("missing.trait.implementations1",
            "mix.traits.implementation",
            "clashing.implementations",
            "additional.traits.by",
            "bad.trait.by.variables",
            "trait.with.trait.by",
            "bad.directtraitcalls"));
  }

}
