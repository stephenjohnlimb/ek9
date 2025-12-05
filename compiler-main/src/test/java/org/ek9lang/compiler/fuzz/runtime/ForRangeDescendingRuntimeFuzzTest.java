package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: For-range with descending range (start > end).
 *
 * <p>Tests the edge case where start is greater than end in a for-range loop.
 * The three-way dispatch should go to the "descending" branch, and the loop
 * should iterate downward using decrement operations.</p>
 *
 * <p>Range 10 ... 1 should sum to 55 (10+9+8+7+6+5+4+3+2+1).</p>
 */
class ForRangeDescendingRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public ForRangeDescendingRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/forRangeDescending",
        "fuzz.runtime.forrange.descending",
        "ForRangeDescending",
        List.of(new SymbolCountCheck("fuzz.runtime.forrange.descending", 1)));
  }
}
