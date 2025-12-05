package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Inner finally throws exception.
 *
 * <p>Tests that exception from finally block propagates to outer catch.
 * The try body completes normally but finally throws.</p>
 */
class TryNestedFinallyThrowsRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryNestedFinallyThrowsRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryNestedFinallyThrows",
        "fuzz.runtime.try.nested.finallythrows",
        "TryNestedFinallyThrows",
        List.of(new SymbolCountCheck("fuzz.runtime.try.nested.finallythrows", 1)));
  }
}
