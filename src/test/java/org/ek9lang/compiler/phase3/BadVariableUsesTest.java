package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Tests bad variable usage, like use before definition, not initialised etc.
 * Now also check for duplicated properties fields in record hierarchy.
 */
class BadVariableUsesTest extends PhasesTest {

  public BadVariableUsesTest() {
    super("/examples/parseButFailCompile/badVariableUses",
        List.of("bad.blockvariable.uses", "bad.duplicateproperties.uses"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
