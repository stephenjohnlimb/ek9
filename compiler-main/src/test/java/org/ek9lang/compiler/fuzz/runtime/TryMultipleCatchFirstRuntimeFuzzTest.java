package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Multiple catch blocks, first one matches.
 *
 * <p>Tests that when multiple catch blocks exist, the FIRST matching
 * catch executes and subsequent catches are skipped.</p>
 */
class TryMultipleCatchFirstRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryMultipleCatchFirstRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryMultipleCatchFirst",
        "fuzz.runtime.try.multicatch.first",
        "TryMultipleCatchFirst",
        List.of(new SymbolCountCheck("fuzz.runtime.try.multicatch.first", 2)));
  }
}
