package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic &lt; operator - both objects fully set.
 *
 * <p>Tests that the synthetic &lt; operator (which delegates to &lt;=&gt;) works correctly.</p>
 */
class SyntheticLtBothSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticLtBothSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticLtBothSet",
        "fuzz.runtime.synthetic.lt.bothset",
        "SyntheticLtBothSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.lt.bothset", 2)));
  }
}
