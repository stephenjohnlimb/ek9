package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Bug #3 Variation - Mixed handlers at different levels.
 *
 * <p>Tests pattern where middle level has catch that re-throws,
 * inner level has finally only, outer level has catch AND finally.
 * Exception propagates through all levels.</p>
 *
 * <p>Pattern: try { try { try { THROW } finally } catch { RETHROW } } catch finally</p>
 */
class TryBug3MixedHandlersRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryBug3MixedHandlersRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryBug3MixedHandlers",
        "fuzz.runtime.try.bug3.mixedhandlers",
        "TryBug3MixedHandlers",
        List.of(new SymbolCountCheck("fuzz.runtime.try.bug3.mixedhandlers", 1)));
  }
}
