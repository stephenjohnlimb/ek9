package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for synthetic _copy (:=:) operator.
 * This test validates that 'default operator :=:' generates correct bytecode
 * that performs a shallow copy of field values from source to target.
 * <p>
 * Tests:
 * - Synthetic _copy operator generation from 'default operator :=:'
 * - Fields are correctly copied from source to target
 * - Shallow copy semantics (references copied, not cloned)
 * </p>
 * <p>
 * Expected output when executed:
 * "PASS: Copy successful"
 * </p>
 */
class SyntheticCopyTest extends AbstractExecutableBytecodeTest {

  public SyntheticCopyTest() {
    super("/examples/bytecodeGeneration/syntheticCopy",
        "bytecode.test",
        "SyntheticCopy",
        List.of(new SymbolCountCheck("bytecode.test", 2)));
  }
}
