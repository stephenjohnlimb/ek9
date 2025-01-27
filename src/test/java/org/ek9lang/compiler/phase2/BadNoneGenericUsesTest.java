package org.ek9lang.compiler.phase2;

import java.util.List;

/**
 * Just tests bad generic uses of generics.
 */
class BadNoneGenericUsesTest extends BadExplicitTypeDefinitionTest {

  public BadNoneGenericUsesTest() {
    super("/examples/parseButFailCompile/badNoneGenericUses",
        List.of("bad.use.non.generic"));
  }
}
