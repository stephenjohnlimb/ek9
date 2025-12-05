package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: 3-level nested try with all finally blocks.
 *
 * <p>Tests deeply nested exception propagation with multiple finally blocks.
 * All three finally blocks must execute in inside-out order.</p>
 */
class TryNested3LevelFinallyRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryNested3LevelFinallyRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryNested3LevelFinally",
        "fuzz.runtime.try.nested.threelevel",
        "TryNested3LevelFinally",
        List.of(new SymbolCountCheck("fuzz.runtime.try.nested.threelevel", 1)));
  }
}
