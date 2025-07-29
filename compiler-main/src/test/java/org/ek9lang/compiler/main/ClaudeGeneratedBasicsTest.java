package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;

/**
 * Just test claude generated ek9 compiles.
 */
class ClaudeGeneratedBasicsTest extends SuccessfulTest {

  public ClaudeGeneratedBasicsTest() {
    super("/claude/basics", false, true);
  }


  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
  }
}
