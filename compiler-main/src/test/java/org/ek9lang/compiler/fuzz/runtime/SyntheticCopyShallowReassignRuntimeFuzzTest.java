package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic :=: operator - shallow copy reassignment test.
 *
 * <p>Tests that after shallow copy, reassigning src's nested field to a new instance
 * does NOT affect dst (reassignment breaks the shared reference link).</p>
 */
class SyntheticCopyShallowReassignRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCopyShallowReassignRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCopyShallowReassign",
        "fuzz.runtime.synthetic.copy.shallowreassign",
        "SyntheticCopyShallowReassign",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.copy.shallowreassign", 3)));
  }
}
