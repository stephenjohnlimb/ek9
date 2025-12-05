package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Multiple catch blocks, base Exception catches unknown type.
 *
 * <p>Tests that when specific catches don't match, the base Exception catch
 * acts as a fallback and catches the exception polymorphically.</p>
 */
class TryMultipleCatchFallbackRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryMultipleCatchFallbackRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryMultipleCatchFallback",
        "fuzz.runtime.try.multicatch.fallback",
        "TryMultipleCatchFallback",
        List.of(new SymbolCountCheck("fuzz.runtime.try.multicatch.fallback", 3)));
  }
}
