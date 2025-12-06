package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic ? operator - last field set.
 *
 * <p>Tests that when the last field is set (but others unset),
 * the synthetic {@code ?} operator returns true (ANY field set = valid).</p>
 */
class SyntheticIsSetLastFieldSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticIsSetLastFieldSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticIsSetLastFieldSet",
        "fuzz.runtime.synthetic.isset.lastfieldset",
        "SyntheticIsSetLastFieldSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.isset.lastfieldset", 2)));
  }
}
