package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad generic parameters count on generics.
 */
class BadGenericParametersTest extends BadSymbolDefinitionTest {

  public BadGenericParametersTest() {
    super("/examples/parseButFailCompile/badGenericDefinitions",
        List.of("incorrect.parameters.on.constructors", "bad.result.parameterization"));
  }
}
