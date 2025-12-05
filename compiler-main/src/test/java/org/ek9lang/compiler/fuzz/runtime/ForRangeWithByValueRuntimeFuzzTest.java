package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: For-range loop with explicit 'by' increment value.
 *
 * <p>Tests that for-range loops with explicit 'by' clause correctly
 * use the specified increment value (using += mutating operator).</p>
 *
 * <p>This test validates the fix for the mutating operator return type bug
 * where += was incorrectly assumed to return the operand type instead of void.</p>
 */
class ForRangeWithByValueRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public ForRangeWithByValueRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/forRangeWithByValue",
        "fuzz.runtime.forrange.byvalue",
        "ForRangeWithByValue",
        List.of(new SymbolCountCheck("fuzz.runtime.forrange.byvalue", 1)));
  }
}
