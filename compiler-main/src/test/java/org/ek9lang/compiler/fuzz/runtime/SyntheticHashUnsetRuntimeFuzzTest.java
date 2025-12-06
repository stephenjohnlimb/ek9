package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic hash operator on unset object.
 *
 * <p>Tests that an unset object returns an unset hash code,
 * maintaining tri-state consistency.</p>
 */
class SyntheticHashUnsetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticHashUnsetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticHashUnset",
        "fuzz.runtime.synthetic.hash.unset",
        "SyntheticHashUnset",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.hash.unset", 2)));
  }
}
