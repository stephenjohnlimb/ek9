package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests classes and inference with generics.
 */
class BadGenericClassesWithInferenceTest extends BadSymbolDefinitionTest {

  public BadGenericClassesWithInferenceTest() {
    super("/examples/parseButFailCompile/badGenericClasses",
        List.of("bad.classes.inference.example", "bad.genericnotparameterised.example"));
  }
}
