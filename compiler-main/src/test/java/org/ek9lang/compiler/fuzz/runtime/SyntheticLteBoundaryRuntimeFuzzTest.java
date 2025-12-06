package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic &lt;= operator - boundary case with equal values.
 *
 * <p>Tests that the synthetic &lt;= operator returns true when values are equal.</p>
 */
class SyntheticLteBoundaryRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticLteBoundaryRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticLteBoundary",
        "fuzz.runtime.synthetic.lte.boundary",
        "SyntheticLteBoundary",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.lte.boundary", 2)));
  }
}
