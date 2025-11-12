package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for try-with-resources with multiple resources.
 * Validates reverse-order resource cleanup (resource2 then resource1).
 *
 * <p>Key Validations:</p>
 * <ul>
 *   <li>Both resources initialized in declaration order (first, second)</li>
 *   <li>Close() calls execute in REVERSE order (second, first)</li>
 *   <li>Exception table covers both resource initializations</li>
 *   <li>Finally block contains both close() calls in reverse order</li>
 *   <li>LocalVariableTable shows both resources with correct scopes</li>
 * </ul>
 */
class TryWithMultipleResourcesTest extends AbstractBytecodeGenerationTest {

  public TryWithMultipleResourcesTest() {
    // Module: bytecode.test.multi
    // Expected symbols: TestResource class (1) + TryWithMultipleResources program (1) = 2
    super("/examples/bytecodeGeneration/tryWithMultipleResources",
        List.of(new SymbolCountCheck("bytecode.test.multi", 2)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
