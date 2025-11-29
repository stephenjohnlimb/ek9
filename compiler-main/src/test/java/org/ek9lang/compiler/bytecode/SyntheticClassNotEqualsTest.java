package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for synthetic _neq ({@literal <>}) operator.
 *
 * <p>This test validates that the not-equals operator:</p>
 * <ul>
 *   <li>Correctly delegates to _eq and negates the result</li>
 *   <li>Returns false when objects are equal</li>
 *   <li>Returns true when objects are not equal</li>
 *   <li>Is always the logical opposite of ==</li>
 * </ul>
 */
class SyntheticClassNotEqualsTest extends AbstractExecutableBytecodeTest {

  public SyntheticClassNotEqualsTest() {
    super("/examples/bytecodeGeneration/syntheticClassNotEquals",
        "bytecode.syntheticClassNotEquals",
        "TestSyntheticClassNotEquals",
        List.of(new SymbolCountCheck("bytecode.syntheticClassNotEquals", 2)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
