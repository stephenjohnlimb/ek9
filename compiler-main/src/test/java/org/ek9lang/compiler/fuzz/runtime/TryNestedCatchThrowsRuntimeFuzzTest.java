package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Inner catch throws new exception.
 *
 * <p>Tests that when inner catch throws, outer catch handles the NEW exception.
 * This verifies exception replacement behavior.</p>
 */
class TryNestedCatchThrowsRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryNestedCatchThrowsRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryNestedCatchThrows",
        "fuzz.runtime.try.nested.catchthrows",
        "TryNestedCatchThrows",
        List.of(new SymbolCountCheck("fuzz.runtime.try.nested.catchthrows", 1)));
  }
}
