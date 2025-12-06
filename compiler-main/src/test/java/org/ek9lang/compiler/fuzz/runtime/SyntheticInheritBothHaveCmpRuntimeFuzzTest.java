package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic comparison with inheritance - both parent and child have <=>.
 *
 * <p>Tests that when both parent and child define 'default operator <=>', the comparison
 * first uses super._cmp() for parent fields, then compares child fields only if
 * the parent comparison returns 0 (equal).</p>
 */
class SyntheticInheritBothHaveCmpRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticInheritBothHaveCmpRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticInheritBothHaveCmp",
        "fuzz.runtime.synthetic.inherit.bothhavecmp",
        "SyntheticInheritBothHaveCmp",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.inherit.bothhavecmp", 3)));
  }
}
