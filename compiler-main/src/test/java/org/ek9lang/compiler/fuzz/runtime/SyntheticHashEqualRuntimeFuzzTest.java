package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic hash operator consistency with equality.
 *
 * <p>Tests that equal objects produce the same hash code, which is required
 * for correct behavior in hash-based collections.</p>
 */
class SyntheticHashEqualRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticHashEqualRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticHashEqual",
        "fuzz.runtime.synthetic.hash.equal",
        "SyntheticHashEqual",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.hash.equal", 2)));
  }
}
