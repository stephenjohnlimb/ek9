package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: For-range with empty range (start == end).
 *
 * <p>Tests the edge case where start equals end in a for-range loop.
 * The three-way dispatch should go to the "equal" branch, and the loop
 * body should execute exactly ONCE with the loop variable equal to that value.</p>
 *
 * <p>This validates that the polymorphic for-range dispatch correctly handles
 * the boundary condition where the range contains only a single value.</p>
 */
class ForRangeEmptyRangeRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public ForRangeEmptyRangeRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/forRangeEmptyRange",
        "fuzz.runtime.forrange.emptyrange",
        "ForRangeEmptyRange",
        List.of(new SymbolCountCheck("fuzz.runtime.forrange.emptyrange", 1)));
  }
}
