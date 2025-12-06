package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic ? operator - all fields unset.
 *
 * <p>Tests that when all fields are unset, the synthetic {@code ?} operator
 * returns false (object is considered "empty").</p>
 */
class SyntheticIsSetAllUnsetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticIsSetAllUnsetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticIsSetAllUnset",
        "fuzz.runtime.synthetic.isset.allunset",
        "SyntheticIsSetAllUnset",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.isset.allunset", 2)));
  }
}
