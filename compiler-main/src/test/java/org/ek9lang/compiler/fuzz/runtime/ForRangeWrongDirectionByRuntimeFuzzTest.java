package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: For-range loop with 'by' in wrong direction.
 *
 * <p>Tests that for-range loops with 'by' clause in wrong direction
 * (positive range with negative increment) correctly produces no output.</p>
 *
 * <p>Expected: No iterations because 1 ... 10 by -1 is impossible</p>
 */
class ForRangeWrongDirectionByRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public ForRangeWrongDirectionByRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/forRangeWrongDirectionBy",
        "fuzz.runtime.forrange.wrongdirectionby",
        "ForRangeWrongDirectionBy",
        List.of(new SymbolCountCheck("fuzz.runtime.forrange.wrongdirectionby", 1)));
  }
}
