package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple class definition.
 * This test validates that basic class structures (fields, constructor, methods)
 * are correctly generated in JVM bytecode and that the bytecode executes correctly.
 * <p>
 * Tests:
 * - Class definition bytecode generation
 * - Field declaration and initialization
 * - Parameterized constructor with field assignment
 * - Getter method (pure, returns field value)
 * - Mutating method (modifies field state)
 * - Class instantiation from program
 * - Method invocation and return values
 * </p>
 * <p>
 * Expected output when executed:
 * "Test 1: Constructor and getter"
 * "42"
 * "Test 2: Mutating method"
 * "43"
 * "All tests passed"
 * </p>
 */
class SimpleClassTest extends AbstractBytecodeGenerationTest {

  public SimpleClassTest() {
    // Each bytecode test gets its own directory for parallel execution safety
    // Module name: bytecode.test
    // Expected symbols: 1 program (TestSimpleClass) + 1 class (SimpleClass) = 2
    super("/examples/bytecodeGeneration/simpleClass",
        List.of(new SymbolCountCheck("bytecode.test", 2)),
        false, false, false);
  }

  /**
   * Disable debug instrumentation for minimal bytecode output.
   */
  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
