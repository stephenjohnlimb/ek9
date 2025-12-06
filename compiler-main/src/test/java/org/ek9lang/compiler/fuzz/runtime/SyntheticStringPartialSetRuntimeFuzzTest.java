package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic string operator - partial fields set.
 *
 * <p>Tests that the synthetic {@code $} operator produces the expected format
 * when some fields are set and some are unset: "ClassName(field1=value, field2=?)".</p>
 */
class SyntheticStringPartialSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticStringPartialSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticStringPartialSet",
        "fuzz.runtime.synthetic.string.partialset",
        "SyntheticStringPartialSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.string.partialset", 2)));
  }
}
