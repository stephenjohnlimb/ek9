package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: For-range descending with wrong direction 'by' value.
 *
 * <p>Tests that for-range loops with descending range (10 ... 1) but
 * positive 'by' value (+2) correctly produces no iterations.</p>
 *
 * <p>Expected: No iterations because 10 ... 1 by 2 is impossible</p>
 */
class ForRangeDescendingWrongByRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public ForRangeDescendingWrongByRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/forRangeDescendingWrongBy",
        "fuzz.runtime.forrange.descendingwrongby",
        "ForRangeDescendingWrongBy",
        List.of(new SymbolCountCheck("fuzz.runtime.forrange.descendingwrongby", 1)));
  }
}
