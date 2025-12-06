package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic &lt; operator - one object unset.
 *
 * <p>Tests that when one object is unset, the &lt; operator returns UNSET.</p>
 */
class SyntheticLtOneUnsetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticLtOneUnsetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticLtOneUnset",
        "fuzz.runtime.synthetic.lt.oneunset",
        "SyntheticLtOneUnset",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.lt.oneunset", 2)));
  }
}
