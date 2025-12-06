package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic ? operator - all fields set.
 *
 * <p>Tests that when all fields are set, the synthetic {@code ?} operator
 * returns true.</p>
 */
class SyntheticIsSetAllSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticIsSetAllSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticIsSetAllSet",
        "fuzz.runtime.synthetic.isset.allset",
        "SyntheticIsSetAllSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.isset.allset", 2)));
  }
}
