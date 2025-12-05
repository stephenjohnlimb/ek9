package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: FOR-IN loop with empty collection.
 *
 * <p>Tests that the FOR-IN loop body never executes when iterating
 * over an empty list. The iterator's hasNext() should return false
 * immediately.</p>
 *
 * <p>This verifies correct iterator handling: empty collections
 * should result in zero iterations.</p>
 */
class ForInEmptyListRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public ForInEmptyListRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/forInEmptyList",
        "fuzz.runtime.forin.emptylist",
        "ForInEmptyList",
        List.of(new SymbolCountCheck("fuzz.runtime.forin.emptylist", 1)));
  }
}
