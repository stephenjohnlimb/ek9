package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic comparison operator - multi-field short-circuit.
 *
 * <p>Tests that multi-field comparison short-circuits correctly:
 * If first field comparison returns non-zero, don't compare second field.
 * If first field comparison returns zero, continue to second field.</p>
 *
 * <p>This verifies the lexicographic comparison order.</p>
 */
class SyntheticCmpMultiFieldRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCmpMultiFieldRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCmpMultiField",
        "fuzz.runtime.synthetic.cmp.multifield",
        "SyntheticCmpMultiField",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.cmp.multifield", 2)));
  }
}
