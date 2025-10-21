package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test that FOR_RANGE throws AssertionError when start value is unset.
 * <p>
 * Validates ASSERT bytecode generation for start._isSet() check in loop initialization.
 * </p>
 * <p>
 * Test scenario:
 * </p>
 * <pre>
 * unsetStart <- Integer()  // Unset value
 * for i in unsetStart ... 10
 *   sum := sum + i
 * </pre>
 * <p>
 * Expected behavior:
 * </p>
 * <ul>
 *   <li>Compilation: SUCCESS (IR and bytecode generation complete)</li>
 *   <li>Bytecode: Contains ASSERT instruction checking start._isSet()</li>
 *   <li>Runtime: Would throw AssertionError before loop body executes</li>
 * </ul>
 */
class ForRangeUnsetStartTest extends AbstractBytecodeGenerationTest {

  public ForRangeUnsetStartTest() {
    super("/examples/bytecodeGeneration/forRangeAssertions/forRangeUnsetStart",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }
}
