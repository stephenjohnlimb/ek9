package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic not-equals operator as inverse of equality.
 *
 * <p>Tests that the {@code <>} operator is the logical inverse of {@code ==}:</p>
 * <ul>
 *   <li>If a == b is true, then a <> b should be false</li>
 *   <li>If a == b is false, then a <> b should be true</li>
 *   <li>If a == b is unset, then a <> b should be unset</li>
 * </ul>
 */
class SyntheticNeqInverseRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticNeqInverseRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticNeqInverse",
        "fuzz.runtime.synthetic.neq.inverse",
        "SyntheticNeqInverse",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.neq.inverse", 2)));
  }
}
