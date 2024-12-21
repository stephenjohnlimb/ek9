package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad pure usage.
 */
class BadPureUseTest extends PhasesTest {

  public BadPureUseTest() {
    super("/examples/parseButFailCompile/badPureUse",
        List.of("bad.pure.scenarios1",
            "bad.pure.scenarios2",
            "bad.pure.expressions",
            "bad.pure.declarations",
            "bad.pure.delegate.scenarios1",
            "bad.pure.delegate.scenarios2",
            "bad.pure.text.components"), false, true);
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
