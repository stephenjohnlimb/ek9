package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for while statement with guard only (implicit isSet check).
 */
class SimpleWhileWithGuardTest extends AbstractBytecodeGenerationTest {
  public SimpleWhileWithGuardTest() {
    super("/examples/bytecodeGeneration/simpleWhileWithGuard",
        List.of(new SymbolCountCheck("bytecode.test.whileguard", 1)),
        false, false, false);  // showBytecode=false - @BYTECODE directive in place
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
