package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Multiple catch blocks, second one matches.
 *
 * <p>Tests that when first catch doesn't match, second catch is tried.
 * Verifies catch ordering when first catch type doesn't match.</p>
 */
class TryMultipleCatchSecondRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryMultipleCatchSecondRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryMultipleCatchSecond",
        "fuzz.runtime.try.multicatch.second",
        "TryMultipleCatchSecond",
        List.of(new SymbolCountCheck("fuzz.runtime.try.multicatch.second", 3)));
  }
}
