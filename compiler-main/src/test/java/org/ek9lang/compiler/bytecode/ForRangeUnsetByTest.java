package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test that FOR_RANGE throws AssertionError when BY value is unset.
 * <p>
 * Validates ASSERT bytecode generation for by._isSet() check in loop initialization.
 * </p>
 * <p>
 * Test scenario:
 * </p>
 * <pre>
 * unsetBy <- Integer()  // Unset value
 * for i in 1 ... 10 by unsetBy
 *   sum := sum + i
 * </pre>
 * <p>
 * Expected behavior:
 * </p>
 * <ul>
 *   <li>Compilation: SUCCESS (IR and bytecode generation complete)</li>
 *   <li>Bytecode: Contains ASSERT instruction checking by._isSet()</li>
 *   <li>Runtime: Would throw AssertionError before loop body executes</li>
 * </ul>
 */
class ForRangeUnsetByTest extends AbstractBytecodeGenerationTest {

  public ForRangeUnsetByTest() {
    super("/examples/bytecodeGeneration/forRangeAssertions/forRangeUnsetBy",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, true);
  }

}
