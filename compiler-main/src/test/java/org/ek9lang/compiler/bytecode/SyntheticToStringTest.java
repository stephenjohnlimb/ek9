package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for synthetic _string ($) operator.
 * This test validates that 'default operator $' generates correct bytecode
 * that produces a string representation of the class.
 * <p>
 * Tests:
 * - Synthetic _string operator generation from 'default operator $'
 * - Correct format: "ClassName(field1=value1, field2=value2)"
 * - All fields included in output
 * </p>
 * <p>
 * Expected output when executed:
 * "Product(name=Widget, price=100)"
 * </p>
 */
class SyntheticToStringTest extends AbstractExecutableBytecodeTest {

  public SyntheticToStringTest() {
    super("/examples/bytecodeGeneration/syntheticToString",
        "bytecode.test",
        "SyntheticToString",
        List.of(new SymbolCountCheck("bytecode.test", 2)));
  }
}
