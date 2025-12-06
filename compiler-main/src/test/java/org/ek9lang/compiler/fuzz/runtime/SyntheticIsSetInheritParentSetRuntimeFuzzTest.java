package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic ? operator - parent field set, child unset.
 *
 * <p>Tests that when the parent class has a field set but child has none,
 * the synthetic {@code ?} operator returns true (via super.?).</p>
 */
class SyntheticIsSetInheritParentSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticIsSetInheritParentSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticIsSetInheritParentSet",
        "fuzz.runtime.synthetic.isset.inheritparentset",
        "SyntheticIsSetInheritParentSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.isset.inheritparentset", 3)));
  }
}
