package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: For-range with unset start variable.
 *
 * <p>Tests that the compiler generates correct ASSERT bytecode that throws
 * AssertionError when the start value's _isSet() returns false.</p>
 *
 * <p>This validates EK9's tri-state semantics: variables can be present but unset,
 * and for-range loops must detect this at runtime before iteration begins.</p>
 */
class ForRangeUnsetStartRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public ForRangeUnsetStartRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/forRangeUnsetStart",
        "fuzz.runtime.forrange.unsetstart",
        "ForRangeUnsetStart",
        List.of(new SymbolCountCheck("fuzz.runtime.forrange.unsetstart", 1)));
  }
}
