package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Synthetic comparison - multi-level inheritance.
 *
 * <p>Tests that synthetic comparison works correctly with three levels of inheritance:
 * grandparent (Entity) → parent (Animal) → child (Cat). Comparison should be
 * lexicographic through all levels.</p>
 */
class SyntheticInheritMultiLevelRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SyntheticInheritMultiLevelRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/syntheticInheritMultiLevel",
        "fuzz.runtime.synthetic.inherit.multilevel",
        "SyntheticInheritMultiLevel",
        List.of(new SymbolCountCheck("fuzz.runtime.synthetic.inherit.multilevel", 4)));
  }
}
