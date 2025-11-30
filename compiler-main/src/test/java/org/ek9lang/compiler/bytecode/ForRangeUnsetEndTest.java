package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test that FOR_RANGE throws AssertionError when end value is unset.
 * <p>
 * Validates ASSERT bytecode generation for end._isSet() check in loop initialization.
 * </p>
 * <p>
 * Test scenario:
 * </p>
 * <pre>
 * unsetEnd <- Integer()  // Unset value
 * for i in 1 ... unsetEnd
 *   sum := sum + i
 * </pre>
 * <p>
 * Expected behavior:
 * </p>
 * <ul>
 *   <li>Compilation: SUCCESS (IR and bytecode generation complete)</li>
 *   <li>Bytecode: Contains ASSERT instruction checking end._isSet()</li>
 *   <li>Runtime: Would throw AssertionError before loop body executes</li>
 * </ul>
 */
class ForRangeUnsetEndTest extends AbstractExecutableBytecodeTest {

  public ForRangeUnsetEndTest() {
    super("/examples/bytecodeGeneration/forRangeAssertions/forRangeUnsetEnd",
        "bytecode.test",
        "ForRangeUnsetEnd",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}
