package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Finally runs even when catch throws.
 *
 * <p>Tests the critical guarantee: finally ALWAYS executes, even when
 * the catch block throws a new exception.</p>
 */
class TryFinallyCatchThrowsRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryFinallyCatchThrowsRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryFinallyCatchThrows",
        "fuzz.runtime.try.finally.catchthrows",
        "TryFinallyCatchThrows",
        List.of(new SymbolCountCheck("fuzz.runtime.try.finally.catchthrows", 1)));
  }
}
