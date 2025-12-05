package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: 2-level nested try, inner has only finally.
 *
 * <p>Tests that exception propagates through inner finally to outer catch.
 * Inner finally MUST run before outer catch executes.</p>
 */
class TryNestedPropagatesRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryNestedPropagatesRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryNestedPropagates",
        "fuzz.runtime.try.nested.propagates",
        "TryNestedPropagates",
        List.of(new SymbolCountCheck("fuzz.runtime.try.nested.propagates", 1)));
  }
}
