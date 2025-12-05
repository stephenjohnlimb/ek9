package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic comparison operator - other is unset.
 *
 * <p>Tests that when 'other' object is not fully set (isSet returns false),
 * the synthetic {@code <=>} operator returns an UNSET result.</p>
 *
 * <p>This tests the tri-state behavior: unset objects cannot be compared.</p>
 */
class SyntheticCmpOtherUnsetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCmpOtherUnsetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCmpOtherUnset",
        "fuzz.runtime.synthetic.cmp.otherunset",
        "SyntheticCmpOtherUnset",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.cmp.otherunset", 2)));
  }
}
