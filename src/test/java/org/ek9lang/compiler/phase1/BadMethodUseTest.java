package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad method usage on a range of constructs and functions.
 */
class BadMethodUseTest extends PhasesTest {

  public BadMethodUseTest() {
    super("/examples/parseButFailCompile/badMethodAndFunctionUse",
        List.of("bad.programs.examples",
            "bad.traits.examples",
            "bad.classes.examples",
            "bad.components.examples",
            "bad.records.examples",
            "bad.functions.examples",
            "bad.dynamicclasses.examples"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
