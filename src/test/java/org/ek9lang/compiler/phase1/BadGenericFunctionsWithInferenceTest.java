package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests functions and inference with generics.
 */
class BadGenericFunctionsWithInferenceTest extends BadSymbolDefinitionTest {

  public BadGenericFunctionsWithInferenceTest() {
    super("/examples/parseButFailCompile/badGenericFunctions",
        List.of("bad.functions.inference.example"));
  }
}
