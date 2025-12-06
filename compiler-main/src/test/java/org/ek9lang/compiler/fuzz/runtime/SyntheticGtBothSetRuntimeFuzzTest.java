package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic &gt; operator - both objects fully set.
 *
 * <p>Tests that the synthetic &gt; operator (which delegates to &lt;=&gt;) works correctly.</p>
 */
class SyntheticGtBothSetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticGtBothSetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticGtBothSet",
        "fuzz.runtime.synthetic.gt.bothset",
        "SyntheticGtBothSet",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.gt.bothset", 2)));
  }
}
