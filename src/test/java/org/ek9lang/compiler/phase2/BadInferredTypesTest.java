package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad property/return type inference.
 * Properties on aggregates in EK9 can have their type inferred but only in a simple way.
 * Return values can also have simple inferences.
 */
class BadInferredTypesTest extends PhasesTest {

  public BadInferredTypesTest() {
    super("/examples/parseButFailCompile/badInferredTypes",
        List.of("bad.inferred.properties", "bad.inferred.returns"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
