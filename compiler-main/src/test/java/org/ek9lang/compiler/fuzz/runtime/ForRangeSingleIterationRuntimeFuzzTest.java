package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: For-range single iteration verification.
 *
 * <p>Baseline sanity check that verifies a simple ascending range
 * iterates the correct number of times with correct values.</p>
 *
 * <p>Range 1 ... 3 should output 1, 2, 3 in order.</p>
 */
class ForRangeSingleIterationRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public ForRangeSingleIterationRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/forRangeSingleIteration",
        "fuzz.runtime.forrange.singleiteration",
        "ForRangeSingleIteration",
        List.of(new SymbolCountCheck("fuzz.runtime.forrange.singleiteration", 1)));
  }
}
