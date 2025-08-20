package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed in testing IR generation for basic assignments.
 */
class AssignmentStatementTest extends AbstractIRGenerationTest {

  public AssignmentStatementTest() {
    super("/examples/irGeneration/assignmentStatements",
        List.of(new SymbolCountCheck(2, "assignments", 4)
        ), false, false);
  }

}
