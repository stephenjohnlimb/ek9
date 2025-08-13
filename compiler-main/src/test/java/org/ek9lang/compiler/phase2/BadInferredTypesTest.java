package org.ek9lang.compiler.phase2;

import java.util.List;

/**
 * Just tests bad property/return type inference.
 * Properties on aggregates in EK9 can have their type inferred but only in a simple way.
 * Return values can also have simple inferences.
 */
class BadInferredTypesTest extends BadExplicitTypeDefinitionTest {

  public BadInferredTypesTest() {
    super("/examples/parseButFailCompile/phase2/badInferredTypes",
        List.of("bad.inferred.properties", "bad.inferred.returns"));
  }
}
