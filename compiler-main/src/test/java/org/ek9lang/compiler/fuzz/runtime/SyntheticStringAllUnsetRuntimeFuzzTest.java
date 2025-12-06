package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic string operator - all fields unset.
 *
 * <p>Tests that the synthetic {@code $} operator produces the expected format
 * when all fields are unset: "ClassName(field1=?, field2=?)".</p>
 */
class SyntheticStringAllUnsetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticStringAllUnsetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticStringAllUnset",
        "fuzz.runtime.synthetic.string.allunset",
        "SyntheticStringAllUnset",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.string.allunset", 2)));
  }
}
