package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for try-with-resources with multiple resources AND explicit finally.
 * Validates the most complex scenario: automatic resource cleanup + user finally code.
 *
 * <p>Key Validations:</p>
 * <ul>
 *   <li>Both resources initialized in declaration order (first, second)</li>
 *   <li>Close() calls execute in REVERSE order (second, first)</li>
 *   <li>User's explicit finally block executes AFTER resource cleanup</li>
 *   <li>Exception table covers both resource initializations and catch handler</li>
 *   <li>Correct execution order: try → catch → implicit cleanup → explicit finally → rethrow</li>
 * </ul>
 */
class TryWithMultipleResourcesAndFinallyTest extends AbstractBytecodeGenerationTest {

  public TryWithMultipleResourcesAndFinallyTest() {
    // Module: bytecode.test.resources.finally
    // Expected symbols: TestResource class (1) + TryWithMultipleResourcesAndFinally program (1) = 2
    super("/examples/bytecodeGeneration/tryWithMultipleResourcesAndFinally",
        List.of(new SymbolCountCheck("bytecode.test.resources.finally", 2)),
        false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
