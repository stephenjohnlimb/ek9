package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad method usage on a range of constructs and functions.
 */
class BadMethodUseTest extends BadSymbolDefinitionTest {

  public BadMethodUseTest() {
    super("/examples/parseButFailCompile/phase1/badMethodAndFunctionUse",
        List.of("bad.programs.examples",
            "bad.traits.examples",
            "bad.classes.examples",
            "bad.components.examples",
            "bad.records.examples",
            "bad.functions.examples",
            "bad.dynamicclasses.examples"));
  }
}
