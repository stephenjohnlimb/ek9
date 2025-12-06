package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic equality operator - both objects fully set with equal values.
 *
 * <p>Tests that when two objects are both fully set with identical field values,
 * the synthetic {@code ==} operator returns true.</p>
 */
class SyntheticEqBothSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticEqBothSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticEqBothSet",
        "fuzz.runtime.synthetic.eq.bothset",
        "SyntheticEqBothSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.eq.bothset", 2)));
  }
}
