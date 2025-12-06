package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic comparison operators - symmetry verification.
 *
 * <p>Tests that comparison operators maintain proper symmetry:
 * a &lt; b should be equivalent to b &gt; a, etc.</p>
 */
class SyntheticCompareSymmetryRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCompareSymmetryRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCompareSymmetry",
        "fuzz.runtime.synthetic.compare.symmetry",
        "SyntheticCompareSymmetry",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.compare.symmetry", 2)));
  }
}
