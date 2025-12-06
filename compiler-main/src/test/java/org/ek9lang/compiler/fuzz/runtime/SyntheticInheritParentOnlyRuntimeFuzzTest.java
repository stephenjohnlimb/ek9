package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic comparison - only parent has <=>.
 *
 * <p>Tests that when only the parent defines 'default operator <=>', the child
 * inherits it and comparison is based only on parent fields.</p>
 */
class SyntheticInheritParentOnlyRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticInheritParentOnlyRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticInheritParentOnly",
        "fuzz.runtime.synthetic.inherit.parentonly",
        "SyntheticInheritParentOnly",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.inherit.parentonly", 3)));
  }
}
