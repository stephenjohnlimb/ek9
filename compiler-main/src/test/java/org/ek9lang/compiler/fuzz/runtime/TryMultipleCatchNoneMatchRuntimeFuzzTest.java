package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Multiple catch blocks, none match - exception propagates.
 *
 * <p>Tests that when no catch block matches, exception propagates to outer handler.
 * This verifies correct propagation past multiple non-matching catches.</p>
 */
class TryMultipleCatchNoneMatchRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryMultipleCatchNoneMatchRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryMultipleCatchNoneMatch",
        "fuzz.runtime.try.multicatch.nonematch",
        "TryMultipleCatchNoneMatch",
        List.of(new SymbolCountCheck("fuzz.runtime.try.multicatch.nonematch", 3)));
  }
}
