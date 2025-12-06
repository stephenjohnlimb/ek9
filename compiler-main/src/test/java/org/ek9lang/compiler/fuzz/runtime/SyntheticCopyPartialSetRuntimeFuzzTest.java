package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic :=: operator - copy from partially set object.
 *
 * <p>Tests that when copying from an object with some fields set,
 * the partial state is correctly preserved in the destination.</p>
 */
class SyntheticCopyPartialSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCopyPartialSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCopyPartialSet",
        "fuzz.runtime.synthetic.copy.partialset",
        "SyntheticCopyPartialSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.copy.partialset", 2)));
  }
}
