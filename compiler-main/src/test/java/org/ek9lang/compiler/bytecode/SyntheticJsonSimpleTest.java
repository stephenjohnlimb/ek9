package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for synthetic _json ($$) operator.
 * This test validates that 'default operator $$' generates correct bytecode
 * that produces valid JSON output.
 * <p>
 * Tests:
 * - Synthetic _json operator generation from 'default operator $$'
 * - JSON object contains field names as keys
 * - Field values are converted to JSON recursively
 * - Output format: {"name":"Test","value":42}
 * </p>
 */
class SyntheticJsonSimpleTest extends AbstractExecutableBytecodeTest {

  public SyntheticJsonSimpleTest() {
    super("/examples/bytecodeGeneration/syntheticJsonSimple",
        "bytecode.test",
        "SyntheticJsonSimple",
        List.of(new SymbolCountCheck("bytecode.test", 2)));  // SimpleData, SyntheticJsonSimple
  }
}
