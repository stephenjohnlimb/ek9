package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic comparison - inheritance with mixed field set status.
 *
 * <p>Tests that when parent fields are set but child fields are unset (or vice versa),
 * the field set status comparison correctly detects the mismatch and returns UNSET.</p>
 */
class SyntheticInheritMixedSetStatusRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticInheritMixedSetStatusRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticInheritMixedSetStatus",
        "fuzz.runtime.synthetic.inherit.mixedsetstatus",
        "SyntheticInheritMixedSetStatus",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.inherit.mixedsetstatus", 3)));
  }
}
