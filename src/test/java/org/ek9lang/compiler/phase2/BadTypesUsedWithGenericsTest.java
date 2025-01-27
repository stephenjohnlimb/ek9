package org.ek9lang.compiler.phase2;

import java.util.List;

/**
 * Just tests bad types used with generics.
 */
class BadTypesUsedWithGenericsTest extends BadExplicitTypeDefinitionTest {


  public BadTypesUsedWithGenericsTest() {
    super("/examples/parseButFailCompile/unresolvedTypeWithGenerics",
        List.of("bad.generics.use.types"));
  }
}
