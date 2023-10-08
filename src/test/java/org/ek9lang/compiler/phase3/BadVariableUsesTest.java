package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
import org.junit.jupiter.api.Test;

/**
 * Tests bad variable usage, like use before definition, not initialised etc.
 * Now also check for duplicated properties fields in record hierarchy.
 */
class BadVariableUsesTest extends FullCompilationTest {

  public BadVariableUsesTest() {
    super("/examples/parseButFailCompile/badVariableUses");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.blockvariable.uses").isEmpty());
    assertFalse(program.getParsedModules("bad.duplicateproperties.uses").isEmpty());
  }
}
