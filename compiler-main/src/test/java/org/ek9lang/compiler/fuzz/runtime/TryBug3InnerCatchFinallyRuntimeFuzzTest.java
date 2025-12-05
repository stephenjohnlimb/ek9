package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Bug #3 Variation - Inner has both catch AND finally.
 *
 * <p>Tests pattern where inner try has BOTH catch AND finally,
 * and outer try also has BOTH catch AND finally.
 * Exception is caught at inner level, so outer catch should NOT execute.</p>
 *
 * <p>Pattern: try { try { THROW } catch finally } catch finally</p>
 */
class TryBug3InnerCatchFinallyRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryBug3InnerCatchFinallyRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryBug3InnerCatchFinally",
        "fuzz.runtime.try.bug3.innercatchfinally",
        "TryBug3InnerCatchFinally",
        List.of(new SymbolCountCheck("fuzz.runtime.try.bug3.innercatchfinally", 1)));
  }
}
