package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: For-range with negative values.
 *
 * <p>Tests for-range behavior when start and end include negative numbers
 * and the range crosses zero.</p>
 *
 * <p>Range -3 ... 2 should iterate 6 times: -3, -2, -1, 0, 1, 2.</p>
 */
class ForRangeNegativeRangeRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public ForRangeNegativeRangeRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/forRangeNegativeRange",
        "fuzz.runtime.forrange.negativerange",
        "ForRangeNegativeRange",
        List.of(new SymbolCountCheck("fuzz.runtime.forrange.negativerange", 1)));
  }
}
