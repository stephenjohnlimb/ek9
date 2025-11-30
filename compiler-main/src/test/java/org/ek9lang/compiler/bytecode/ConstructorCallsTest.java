package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for constructor initialization patterns.
 * Validates explicit super() calls, constructor delegation, field initialization order.
 * <p>
 * Tests:
 * - Explicit super() calls with parameters
 * - Constructor delegation with this()
 * - Field initialization order (i_init before constructor body)
 * - Explicit super() to synthetic base constructor
 * - Correct bytecode: ALOAD_0 + INVOKESPECIAL for super()
 * - Initialization order: super() -&gt; i_init() -&gt; constructor body
 * </p>
 */
class ConstructorCallsTest extends AbstractExecutableBytecodeTest {

  public ConstructorCallsTest() {
    super("/examples/bytecodeGeneration/constructorCalls",
        "bytecode.test",
        "TestConstructorCalls",
        List.of(new SymbolCountCheck("bytecode.test", 6)));
  }
}
