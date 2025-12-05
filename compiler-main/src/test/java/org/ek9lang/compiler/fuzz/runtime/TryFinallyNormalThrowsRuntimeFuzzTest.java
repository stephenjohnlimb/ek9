package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Finally throws when try succeeds.
 *
 * <p>Tests that exception from finally propagates even when
 * try block completed normally (no exception in try).</p>
 */
class TryFinallyNormalThrowsRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryFinallyNormalThrowsRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryFinallyNormalThrows",
        "fuzz.runtime.try.finally.normalthrows",
        "TryFinallyNormalThrows",
        List.of(new SymbolCountCheck("fuzz.runtime.try.finally.normalthrows", 1)));
  }
}
