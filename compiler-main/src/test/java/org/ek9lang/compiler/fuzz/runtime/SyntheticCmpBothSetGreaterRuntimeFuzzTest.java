package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic comparison operator - this greater than other.
 *
 * <p>Tests that when two objects are both fully set and this &gt; other,
 * the synthetic {@code <=>} operator returns 1.</p>
 */
class SyntheticCmpBothSetGreaterRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCmpBothSetGreaterRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCmpBothSetGreater",
        "fuzz.runtime.synthetic.cmp.bothsetgreater",
        "SyntheticCmpBothSetGreater",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.cmp.bothsetgreater", 2)));
  }
}
