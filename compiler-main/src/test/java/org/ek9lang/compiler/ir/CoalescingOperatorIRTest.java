package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focused on testing IR generation for coalescing operators.
 * Tests null coalescing (??), elvis (:?), and comparison coalescing (<?, >?, <=?, >=?).
 */
class CoalescingOperatorIRTest extends AbstractIRGenerationTest {

  public CoalescingOperatorIRTest() {
    super("/examples/irGeneration/operatorUse/coalescing",
        List.of(
            new SymbolCountCheck(1, "nullCoalesce.test", 1),
            new SymbolCountCheck(1, "elvisCoalesce.test", 1)
        ), false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
