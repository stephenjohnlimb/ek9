package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: WHILE loop with unset guard variable.
 *
 * <p>Tests that the WHILE loop never enters when the guard variable
 * is unset (tri-state semantics). The guard check uses _isSet() at runtime.</p>
 *
 * <p>This verifies EK9's tri-state semantics: when a guard variable is present
 * but unset, the loop body should not execute even once.</p>
 */
class WhileGuardUnsetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public WhileGuardUnsetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/whileGuardUnset",
        "fuzz.runtime.while.guardunset",
        "WhileGuardUnset",
        List.of(new SymbolCountCheck("fuzz.runtime.while.guardunset", 1)));
  }
}
