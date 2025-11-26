package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for comprehensive nested control flow.
 * Validates compilation of deeply nested control flow structures (3 levels) combining:
 * <ul>
 *   <li>while loop with if/else, for-range, and switch nested inside</li>
 *   <li>switch statement with do-while and for-range nested in cases</li>
 *   <li>Complex boolean expressions (and/or) at multiple depths</li>
 *   <li>If/else chains for result categorization</li>
 * </ul>
 *
 * <p>This test validates that the IR generator correctly maintains:
 * <ul>
 *   <li>Unique _temp variables across all nesting levels</li>
 *   <li>Unique _scope_N identifiers without collisions</li>
 *   <li>Proper scope entry/exit when nested</li>
 *   <li>Loop variable isolation at multiple levels</li>
 * </ul>
 *
 * <p>The program accepts two Integer command-line arguments:
 * <ul>
 *   <li>threshold: Numeric range for computations (1-100, validated)</li>
 *   <li>iterations: Number of loop iterations (1-20, validated)</li>
 * </ul>
 */
class ComprehensiveNestedControlFlowTest extends AbstractExecutableBytecodeTest {
  public ComprehensiveNestedControlFlowTest() {
    super("/examples/bytecodeGeneration/comprehensiveNestedControlFlow",
        "bytecode.test.nested",
        "ComprehensiveNestedControlFlow",
        List.of(new SymbolCountCheck("bytecode.test.nested", 1)));
  }
}
