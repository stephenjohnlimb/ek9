package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: For-range with unset BY variable.
 *
 * <p>Tests that the compiler generates correct ASSERT bytecode that throws
 * AssertionError when the by value's _isSet() returns false.</p>
 *
 * <p>This validates EK9's tri-state semantics: variables can be present but unset,
 * and for-range loops must detect this at runtime before iteration begins.</p>
 */
class ForRangeUnsetByRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public ForRangeUnsetByRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/forRangeUnsetBy",
        "fuzz.runtime.forrange.unsetby",
        "ForRangeUnsetBy",
        List.of(new SymbolCountCheck("fuzz.runtime.forrange.unsetby", 1)));
  }
}
