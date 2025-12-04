package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for synthetic _hashcode (#?) operator.
 * This test validates that 'default operator #?' generates correct bytecode
 * that produces a hash code based on field values.
 * <p>
 * Tests:
 * - Synthetic _hashcode operator generation from 'default operator #?'
 * - Identical objects produce the same hash code
 * - Different objects produce different hash codes
 * - Hash code computed using polynomial hash (31 * prev + field.#?)
 * </p>
 */
class SyntheticHashCodeTest extends AbstractExecutableBytecodeTest {

  public SyntheticHashCodeTest() {
    super("/examples/bytecodeGeneration/syntheticHashCode",
        "bytecode.test",
        "SyntheticHashCode",
        List.of(new SymbolCountCheck("bytecode.test", 3)));  // ItemA, ItemB, SyntheticHashCode
  }
}
