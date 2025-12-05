package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic comparison operator - this less than other.
 *
 * <p>Tests that when two objects are both fully set and this &lt; other,
 * the synthetic {@code <=>} operator returns -1.</p>
 */
class SyntheticCmpBothSetLessRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCmpBothSetLessRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCmpBothSetLess",
        "fuzz.runtime.synthetic.cmp.bothsetless",
        "SyntheticCmpBothSetLess",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.cmp.bothsetless", 2)));
  }
}
