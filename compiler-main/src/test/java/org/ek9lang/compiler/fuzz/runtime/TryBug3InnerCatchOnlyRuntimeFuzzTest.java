package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Bug #3 Variation - Inner has catch only (no finally).
 *
 * <p>Tests pattern where inner try has catch only,
 * and outer try has BOTH catch AND finally.
 * Exception is caught at inner level, so outer catch should NOT execute.</p>
 *
 * <p>Pattern: try { try { THROW } catch } catch finally</p>
 */
class TryBug3InnerCatchOnlyRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryBug3InnerCatchOnlyRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryBug3InnerCatchOnly",
        "fuzz.runtime.try.bug3.innercatchonly",
        "TryBug3InnerCatchOnly",
        List.of(new SymbolCountCheck("fuzz.runtime.try.bug3.innercatchonly", 1)));
  }
}
