package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic equality operator - one object unset.
 *
 * <p>Tests that when one object is unset, the synthetic {@code ==} operator
 * returns UNSET, validating tri-state semantics.</p>
 */
class SyntheticEqOneUnsetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticEqOneUnsetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticEqOneUnset",
        "fuzz.runtime.synthetic.eq.oneunset",
        "SyntheticEqOneUnset",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.eq.oneunset", 2)));
  }
}
