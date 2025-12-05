package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Try guard set executes try body and finally.
 *
 * <p>Tests that when try guard is SET, the try body executes normally,
 * and finally also executes.</p>
 *
 * <p>Contrast with TryGuardUnsetSkipsAll - this tests the SET case.</p>
 */
class TryGuardSetExecutesRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryGuardSetExecutesRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryGuardSetExecutes",
        "fuzz.runtime.try.guard.setexecutes",
        "TryGuardSetExecutes",
        List.of(new SymbolCountCheck("fuzz.runtime.try.guard.setexecutes", 1)));
  }
}
