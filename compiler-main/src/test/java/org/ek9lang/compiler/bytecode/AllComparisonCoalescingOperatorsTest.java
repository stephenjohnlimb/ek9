package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Comprehensive test for ALL comparison coalescing operators in a single program.
 * Tests {@code <?, >?, <=?, >=?} operators with various scenarios including:
 * <ul>
 *   <li>Both operands set (comparison evaluation)</li>
 *   <li>LHS/RHS unset handling</li>
 *   <li>Multiple operators in same scope (unique label generation)</li>
 *   <li>Edge cases (equal values, different comparisons)</li>
 * </ul>
 */
class AllComparisonCoalescingOperatorsTest extends AbstractBytecodeGenerationTest {

  public AllComparisonCoalescingOperatorsTest() {
    //Each bytecode test gets its own directory for parallel execution safety
    //Module name: bytecode.test, expected symbol count: 1 (the program)
    super("/examples/bytecodeGeneration/allComparisonCoalescingOperators",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false); // showBytecode disabled for cleaner test output
  }

  /**
   * Disable debug instrumentation for minimal bytecode output.
   */
  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
