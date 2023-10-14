package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Test the basics all compile.
 */
class ExamplesBasicsTest extends PhasesTest {

  public ExamplesBasicsTest() {
    super("/examples/basics");
  }

  @Test
  void testPhaseDevelopment() {
    //TODO move to full resolution.
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertTrue(compilationResult);

    assertFalse(program.getParsedModules("example.placeholder").isEmpty());

    assertFalse(program.getParsedModules("net.customer.extend").isEmpty());
    assertFalse(program.getParsedModules("com.customer.params").isEmpty());
    assertFalse(program.getParsedModules("net.customer.coercions").isEmpty());
    assertFalse(program.getParsedModules("introduction").isEmpty());
    assertFalse(program.getParsedModules("net.customer.inferred").isEmpty());
    assertFalse(program.getParsedModules("com.customer.interpolated").isEmpty());
    assertFalse(program.getParsedModules("com.customer.starter.example").isEmpty());
  }
}
