package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Test the basics all compile.
 */
class ExamplesBasicsTest extends PhasesTest {

  public ExamplesBasicsTest() {
    super("/examples/basics",
        List.of("example.placeholder",
            "net.customer.extend",
            "com.customer.params",
            "net.customer.coercions",
            "introduction",
            "net.customer.inferred",
            "com.customer.interpolated",
            "com.customer.starter.example"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertTrue(compilationResult);

  }
}
