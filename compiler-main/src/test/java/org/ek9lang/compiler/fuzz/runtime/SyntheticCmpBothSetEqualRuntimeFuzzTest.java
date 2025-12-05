package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic comparison operator - both objects fully set with equal values.
 *
 * <p>Tests that when two objects are both fully set with identical field values,
 * the synthetic {@code <=>} operator returns 0 (equal).</p>
 *
 * <p>This validates the happy path for synthetic comparison operators.</p>
 */
class SyntheticCmpBothSetEqualRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCmpBothSetEqualRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCmpBothSetEqual",
        "fuzz.runtime.synthetic.cmp.bothsetequal",
        "SyntheticCmpBothSetEqual",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.cmp.bothsetequal", 2)));
  }
}
