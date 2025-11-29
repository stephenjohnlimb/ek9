package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple while loop.
 * Validates that loop control flow (condition, iteration, exit)
 * is correctly generated in JVM bytecode.
 * <p>
 * Tests:
 * </p>
 * <ul>
 *   <li>Loop start label placement</li>
 *   <li>Condition evaluation and branching</li>
 *   <li>Body execution</li>
 *   <li>Backward jump to loop start (GOTO)</li>
 *   <li>Loop exit label placement</li>
 *   <li>Stdout output (runtime validation: should print 10)</li>
 * </ul>
 */
class SimpleWhileLoopTest extends AbstractExecutableBytecodeTest {

  public SimpleWhileLoopTest() {
    super("/examples/bytecodeGeneration/simpleWhileLoop",
        "bytecode.test",
        "SimpleWhileLoop",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}
