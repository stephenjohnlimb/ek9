package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad generic uses of generics with parameteric types.
 */
class BadGenericInferenceTest extends BadSymbolDefinitionTest {

  public BadGenericInferenceTest() {
    super("/examples/parseButFailCompile/phase1/badGenericNesting",
        List.of("bad.inference.example"));
  }
}
