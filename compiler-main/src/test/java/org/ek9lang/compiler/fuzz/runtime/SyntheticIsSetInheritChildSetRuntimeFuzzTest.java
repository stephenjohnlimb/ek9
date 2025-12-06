package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic ? operator - parent unset, child field set.
 *
 * <p>Tests that when the parent class has no fields set but child has one,
 * the synthetic {@code ?} operator returns true (ANY field set in child = valid).</p>
 */
class SyntheticIsSetInheritChildSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticIsSetInheritChildSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticIsSetInheritChildSet",
        "fuzz.runtime.synthetic.isset.inheritchildset",
        "SyntheticIsSetInheritChildSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.isset.inheritchildset", 3)));
  }
}
