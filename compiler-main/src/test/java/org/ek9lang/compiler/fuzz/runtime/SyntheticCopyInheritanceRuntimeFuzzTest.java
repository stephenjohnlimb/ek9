package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic :=: operator - copy with class inheritance.
 *
 * <p>Tests that when copying objects with inheritance,
 * both parent and child fields are correctly copied.</p>
 */
class SyntheticCopyInheritanceRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticCopyInheritanceRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticCopyInheritance",
        "fuzz.runtime.synthetic.copy.inheritance",
        "SyntheticCopyInheritance",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.copy.inheritance", 3)));
  }
}
