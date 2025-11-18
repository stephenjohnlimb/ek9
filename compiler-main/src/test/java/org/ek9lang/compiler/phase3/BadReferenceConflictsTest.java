package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Tests reference conflicts (REFERENCES_CONFLICT and CONSTRUCT_REFERENCE_CONFLICT).
 * Note: As this uses multiple files and multithreaded compiler, @Error directives
 * may not work reliably. If directive matching fails, this test may need to be
 * converted to use error count validation like BadReferencesTest.
 */
class BadReferenceConflictsTest extends PhasesTest {

  public BadReferenceConflictsTest() {
    super("/examples/parseButFailCompile/phase3/badReferenceConflicts",
        List.of("external.module.for.conflicts",
            "duplicate.references.test",
            "construct.reference.conflict.test"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors,
                                    CompilableProgram program) {
    assertFalse(compilationResult);
    // Expected errors:
    // - 1 REFERENCES_CONFLICT (duplicate reference to SharedClass)
    // - 3 CONSTRUCT_REFERENCE_CONFLICT (SharedClass class, SharedClass constructor, sharedFunction)
    assertEquals(4, numberOfErrors);
  }
}
