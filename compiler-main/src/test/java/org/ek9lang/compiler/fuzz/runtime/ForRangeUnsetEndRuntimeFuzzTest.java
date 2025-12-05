package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: For-range with unset end variable.
 *
 * <p>Tests that the compiler generates correct ASSERT bytecode that throws
 * AssertionError when the end value's _isSet() returns false.</p>
 *
 * <p>This validates EK9's tri-state semantics: variables can be present but unset,
 * and for-range loops must detect this at runtime before iteration begins.</p>
 */
class ForRangeUnsetEndRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public ForRangeUnsetEndRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/forRangeUnsetEnd",
        "fuzz.runtime.forrange.unsetend",
        "ForRangeUnsetEnd",
        List.of(new SymbolCountCheck("fuzz.runtime.forrange.unsetend", 1)));
  }
}
