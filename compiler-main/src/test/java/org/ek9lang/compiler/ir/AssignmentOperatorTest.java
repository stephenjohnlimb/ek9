package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed in testing IR generation for assignment expressions.
 */
class AssignmentOperatorTest extends AbstractIRGenerationTest {

  public AssignmentOperatorTest() {
    super("/examples/irGeneration/operatorUse/assignment",
        List.of(
            new SymbolCountCheck(1, "pointer_assignment.test", 1),
            new SymbolCountCheck(1, "equals_assignment.test", 1),
            new SymbolCountCheck(1, "colon_equals_assignment.test", 1)
        ), false, false, false);
  }

}