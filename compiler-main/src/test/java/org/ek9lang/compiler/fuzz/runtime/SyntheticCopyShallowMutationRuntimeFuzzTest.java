package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic :=: operator - shallow copy mutation test.
 *
 * <p>Tests whether copy is shallow (reference sharing) or deep (value copying)
 * by mutating a nested object after copy and observing the effect.</p>
 */
class SyntheticCopyShallowMutationRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCopyShallowMutationRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCopyShallowMutation",
        "fuzz.runtime.synthetic.copy.shallowmutation",
        "SyntheticCopyShallowMutation",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.copy.shallowmutation", 3)));
  }
}
