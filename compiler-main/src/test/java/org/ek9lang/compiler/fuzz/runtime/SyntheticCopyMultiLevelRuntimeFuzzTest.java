package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic :=: operator - multi-level nesting copy.
 *
 * <p>Tests shallow copy with 3 levels of nesting: Outer -> Middle -> Inner.
 * Proves shallow copy propagates correctly through multiple levels.</p>
 */
class SyntheticCopyMultiLevelRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCopyMultiLevelRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCopyMultiLevel",
        "fuzz.runtime.synthetic.copy.multilevel",
        "SyntheticCopyMultiLevel",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.copy.multilevel", 4)));
  }
}
