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
class ConstructorCallsTest extends AbstractBytecodeGenerationTest {

  public ConstructorCallsTest() {
    // Module: bytecode.test
    // Expected symbols: 5 classes + 1 program = 6
    // Classes: BaseWithParam, ChildWithExplicitSuper, BaseWithSyntheticConstructor,
    //          ChildCallingSynthetic, DelegatingConstructor
    super("/examples/bytecodeGeneration/constructorCalls",
        List.of(new SymbolCountCheck("bytecode.test", 6)),
        false, false, false);  // showBytecode=true - extracting bytecode
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
