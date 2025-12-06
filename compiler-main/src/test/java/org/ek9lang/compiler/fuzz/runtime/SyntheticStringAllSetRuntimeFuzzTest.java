package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic string operator - all fields set.
 *
 * <p>Tests that the synthetic {@code $} operator produces the expected format
 * when all fields are set: "ClassName(field1=value1, field2=value2)".</p>
 */
class SyntheticStringAllSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticStringAllSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticStringAllSet",
        "fuzz.runtime.synthetic.string.allset",
        "SyntheticStringAllSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.string.allset", 2)));
  }
}
