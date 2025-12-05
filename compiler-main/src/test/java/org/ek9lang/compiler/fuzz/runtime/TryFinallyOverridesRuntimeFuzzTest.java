package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Finally exception overrides try exception.
 *
 * <p>Tests that when both try and finally throw, the finally exception
 * propagates (the try exception is replaced/suppressed).</p>
 */
class TryFinallyOverridesRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryFinallyOverridesRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryFinallyOverrides",
        "fuzz.runtime.try.finally.overrides",
        "TryFinallyOverrides",
        List.of(new SymbolCountCheck("fuzz.runtime.try.finally.overrides", 3)));
  }
}
