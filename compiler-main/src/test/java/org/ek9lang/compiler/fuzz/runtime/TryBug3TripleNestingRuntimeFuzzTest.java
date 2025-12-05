package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Bug #3 Variation - Triple nesting with finally at each level.
 *
 * <p>Tests 3-level nesting where exception propagates through multiple finally blocks
 * to outermost catch. All three finally blocks must execute.</p>
 *
 * <p>Pattern: try { try { try { THROW } finally } finally } catch finally</p>
 */
class TryBug3TripleNestingRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryBug3TripleNestingRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryBug3TripleNesting",
        "fuzz.runtime.try.bug3.triplenesting",
        "TryBug3TripleNesting",
        List.of(new SymbolCountCheck("fuzz.runtime.try.bug3.triplenesting", 1)));
  }
}
