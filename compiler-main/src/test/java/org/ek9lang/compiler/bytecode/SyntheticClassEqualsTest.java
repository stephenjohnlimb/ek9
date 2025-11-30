package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for synthetic operators on a simple class.
 * This test validates that the default operator {@literal <=>} and {@literal ==}
 * generate correct JVM bytecode.
 * <p>
 * Tests:
 * - Synthetic _cmp method bytecode generation
 * - Synthetic _eq method bytecode generation
 * - IsSet guards in synthetic operators
 * - Field-by-field comparison logic
 * - Return value generation (true/false/unset)
 * </p>
 * <p>
 * Expected bytecode patterns:
 * - _cmp method returns Integer (0, negative, positive, or unset)
 * - _eq method returns Boolean (true, false, or unset)
 * - Both methods call _isSet() guards on this and other
 * - Field comparisons use invokevirtual for _cmp/_eq
 * </p>
 */
class SyntheticClassEqualsTest extends AbstractExecutableBytecodeTest {

  public SyntheticClassEqualsTest() {
    super("/examples/bytecodeGeneration/syntheticClassEquals",
        "bytecode.syntheticClassEquals",
        "TestSyntheticClassEquals",
        List.of(new SymbolCountCheck("bytecode.syntheticClassEquals", 2)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
