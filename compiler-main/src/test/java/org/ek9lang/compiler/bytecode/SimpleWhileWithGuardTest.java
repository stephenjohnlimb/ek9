package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for while statement with guard only (implicit isSet check).
 */
class SimpleWhileWithGuardTest extends AbstractExecutableBytecodeTest {
  public SimpleWhileWithGuardTest() {
    super("/examples/bytecodeGeneration/simpleWhileWithGuard",
        "bytecode.test.whileguard",
        "SimpleWhileWithGuard",
        List.of(new SymbolCountCheck("bytecode.test.whileguard", 1)));
  }
}
