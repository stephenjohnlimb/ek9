package org.ek9lang.compiler.phase2;

import java.util.List;

/**
 * Just tests bad operator definition/usage on a range of constructs.
 */
class BadOperatorUseTest extends BadExplicitTypeDefinitionTest {

  public BadOperatorUseTest() {
    super("/examples/parseButFailCompile/phase2/badOperatorUse",
        List.of("good.classes.operators.examples",
            "bad.classes.operators.examples1",
            "bad.classes.operators.examples2",
            "bad.classes.operators.examples3",
            "bad.classes.operators.examples4",
            "bad.classes.operators.examples5",
            "bad.dynamicclasses.operators.examples",
            "good.traits.operators.examples",
            "bad.traits.operators.examples",
            "good.components.operators.examples",
            "bad.components.operators.examples",
            "good.records.operators.examples",
            "bad.records.operators.examples",
            "bad.abstractuse.example",
            "missing.operators.examples",
            "bad.defaultoperators.record.examples"));
  }
}
