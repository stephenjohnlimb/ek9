package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic :=: operator - collection field copy.
 *
 * <p>Tests shallow copy behavior with List field.
 * Collections are "always set when created" - does copy share the reference?</p>
 */
class SyntheticCopyCollectionRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCopyCollectionRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCopyCollection",
        "fuzz.runtime.synthetic.copy.collection",
        "SyntheticCopyCollection",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.copy.collection", 2)));
  }
}
