package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for comprehensive nested control flow.
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
 *
 * <h3>Runtime Testing Scenarios</h3>
 * After compilation, the program can be executed with different arguments:
 * <pre>{@code
 * # Valid input - normal execution path
 * ./comprehensiveNestedControlFlow.ek9 10 5
 *
 * # Edge case - minimum values
 * ./comprehensiveNestedControlFlow.ek9 1 1
 *
 * # Edge case - maximum values
 * ./comprehensiveNestedControlFlow.ek9 100 20
 *
 * # Validation trigger - invalid threshold (triggers default 10)
 * ./comprehensiveNestedControlFlow.ek9 0 5
 * ./comprehensiveNestedControlFlow.ek9 101 5
 *
 * # Validation trigger - invalid iterations (triggers default 5)
 * ./comprehensiveNestedControlFlow.ek9 10 0
 * ./comprehensiveNestedControlFlow.ek9 10 21
 *
 * # Both invalid - triggers both defaults
 * ./comprehensiveNestedControlFlow.ek9 -1 -1
 * }</pre>
 */
class ComprehensiveNestedControlFlowTest extends AbstractBytecodeGenerationTest {
  public ComprehensiveNestedControlFlowTest() {
    super("/examples/bytecodeGeneration/comprehensiveNestedControlFlow",
        List.of(new SymbolCountCheck("bytecode.test.nested", 1)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Clean bytecode for easier validation
  }
}
