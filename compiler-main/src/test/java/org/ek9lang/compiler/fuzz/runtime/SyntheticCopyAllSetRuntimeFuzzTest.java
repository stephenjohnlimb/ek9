package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic :=: operator - copy from fully set object.
 *
 * <p>Tests that when copying from an object with all fields set,
 * all fields are correctly copied to the destination.</p>
 */
class SyntheticCopyAllSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCopyAllSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCopyAllSet",
        "fuzz.runtime.synthetic.copy.allset",
        "SyntheticCopyAllSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.copy.allset", 2)));
  }
}
