package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic equality operator - both set but different values.
 *
 * <p>Tests that when two objects are both fully set but have different field values,
 * the synthetic {@code ==} operator returns false.</p>
 */
class SyntheticEqDifferentValuesRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticEqDifferentValuesRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticEqDifferentValues",
        "fuzz.runtime.synthetic.eq.differentvalues",
        "SyntheticEqDifferentValues",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.eq.differentvalues", 2)));
  }
}
