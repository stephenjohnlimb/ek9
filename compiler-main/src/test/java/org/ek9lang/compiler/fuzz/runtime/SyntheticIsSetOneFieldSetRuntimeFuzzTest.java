package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic ? operator - first field set.
 *
 * <p>Tests that when the first field is set (but others unset),
 * the synthetic {@code ?} operator returns true (ANY field set = valid).</p>
 */
class SyntheticIsSetOneFieldSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticIsSetOneFieldSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticIsSetOneFieldSet",
        "fuzz.runtime.synthetic.isset.onefieldset",
        "SyntheticIsSetOneFieldSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.isset.onefieldset", 2)));
  }
}
