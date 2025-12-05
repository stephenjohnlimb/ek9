package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: 2-level nested try, both have finally blocks.
 *
 * <p>Tests that when inner catch handles exception, BOTH finally blocks run.
 * Order must be: inner finally first, then outer finally.</p>
 */
class TryNestedBothFinallyRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryNestedBothFinallyRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryNestedBothFinally",
        "fuzz.runtime.try.nested.bothfinally",
        "TryNestedBothFinally",
        List.of(new SymbolCountCheck("fuzz.runtime.try.nested.bothfinally", 1)));
  }
}
