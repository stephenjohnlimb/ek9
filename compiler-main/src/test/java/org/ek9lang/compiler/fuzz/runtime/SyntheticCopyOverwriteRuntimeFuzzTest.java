package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic :=: operator - copy to already populated object.
 *
 * <p>Tests that when copying to an object that already has values,
 * all fields are overwritten with the source values.</p>
 */
class SyntheticCopyOverwriteRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCopyOverwriteRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCopyOverwrite",
        "fuzz.runtime.synthetic.copy.overwrite",
        "SyntheticCopyOverwrite",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.copy.overwrite", 2)));
  }
}
