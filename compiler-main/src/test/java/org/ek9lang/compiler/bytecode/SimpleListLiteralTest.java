package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for list literal syntax [...].
 * Validates that list literals ["one", "two", "three"] generate correct bytecode.
 * <p>
 * Tests:
 * </p>
 * <ul>
 *   <li>List() constructor call</li>
 *   <li>Multiple _addAss() calls for each element</li>
 *   <li>String literal loading</li>
 *   <li>Iterator-based for-in loop over list</li>
 *   <li>Final stdout output (runtime verification: should print "onetwothree")</li>
 * </ul>
 */
class SimpleListLiteralTest extends AbstractBytecodeGenerationTest {

  public SimpleListLiteralTest() {
    // One directory per test - parallel execution safety
    super("/examples/bytecodeGeneration/simpleListLiteral",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);  // showBytecode = false (final validation)
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;  // Clean bytecode for easier validation
  }
}
