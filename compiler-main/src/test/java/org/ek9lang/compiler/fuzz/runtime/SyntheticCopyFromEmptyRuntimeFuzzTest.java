package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic :=: operator - copy from empty object.
 *
 * <p>Tests that when copying from an empty (unset) source object,
 * the destination becomes empty/unset.</p>
 */
class SyntheticCopyFromEmptyRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCopyFromEmptyRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCopyFromEmpty",
        "fuzz.runtime.synthetic.copy.fromempty",
        "SyntheticCopyFromEmpty",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.copy.fromempty", 2)));
  }
}
