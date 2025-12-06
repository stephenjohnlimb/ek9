package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic :=: operator - copy to empty destination.
 *
 * <p>Tests that when copying to an empty destination object,
 * the destination receives all values from the source.</p>
 */
class SyntheticCopyToEmptyRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCopyToEmptyRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCopyToEmpty",
        "fuzz.runtime.synthetic.copy.toempty",
        "SyntheticCopyToEmpty",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.copy.toempty", 2)));
  }
}
