package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic &gt;= operator - boundary case with equal values.
 *
 * <p>Tests that the synthetic &gt;= operator returns true when values are equal.</p>
 */
class SyntheticGteBoundaryRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticGteBoundaryRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticGteBoundary",
        "fuzz.runtime.synthetic.gte.boundary",
        "SyntheticGteBoundary",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.gte.boundary", 2)));
  }
}
