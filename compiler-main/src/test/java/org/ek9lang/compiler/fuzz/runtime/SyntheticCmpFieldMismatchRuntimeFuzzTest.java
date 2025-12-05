package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic comparison operator - different field set patterns.
 *
 * <p>Tests that when two objects have different fields set (field set status mismatch),
 * the synthetic {@code <=>} operator returns an UNSET result.</p>
 *
 * <p>This is the CRITICAL tri-state test: objects with different set patterns
 * cannot be meaningfully compared, so the result must be unset.</p>
 */
class SyntheticCmpFieldMismatchRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCmpFieldMismatchRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCmpFieldMismatch",
        "fuzz.runtime.synthetic.cmp.fieldmismatch",
        "SyntheticCmpFieldMismatch",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.cmp.fieldmismatch", 2)));
  }
}
