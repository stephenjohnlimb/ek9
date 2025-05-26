package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad generic uses of generics with parameteric types.
 */
class BadGenericInferenceTest extends BadSymbolDefinitionTest {

  public BadGenericInferenceTest() {
    super("/examples/parseButFailCompile/badGenericNesting",
        List.of("bad.inference.example"));
  }
}
