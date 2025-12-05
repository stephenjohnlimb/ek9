package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: 2-level nested try, inner catch handles exception.
 *
 * <p>Tests that when inner try throws and inner catch handles it,
 * the outer catch is NOT triggered.</p>
 */
class TryNestedInnerCaughtRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryNestedInnerCaughtRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryNestedInnerCaught",
        "fuzz.runtime.try.nested.innercaught",
        "TryNestedInnerCaught",
        List.of(new SymbolCountCheck("fuzz.runtime.try.nested.innercaught", 1)));
  }
}
