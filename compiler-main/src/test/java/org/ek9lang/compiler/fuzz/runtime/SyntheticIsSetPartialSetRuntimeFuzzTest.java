package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic ? operator - multiple fields, partial set.
 *
 * <p>Tests that when some fields are set and some are unset,
 * the synthetic {@code ?} operator returns true (ANY field set = valid).</p>
 */
class SyntheticIsSetPartialSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticIsSetPartialSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticIsSetPartialSet",
        "fuzz.runtime.synthetic.isset.partialset",
        "SyntheticIsSetPartialSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.isset.partialset", 2)));
  }
}
