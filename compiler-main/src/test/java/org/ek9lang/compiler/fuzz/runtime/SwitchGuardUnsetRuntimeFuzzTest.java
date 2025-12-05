package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: SWITCH statement with unset guard variable.
 *
 * <p>Tests that the entire SWITCH is skipped when the guard variable
 * is unset (tri-state semantics). No case, including default, should execute.</p>
 *
 * <p>This verifies EK9's tri-state semantics: when a guard variable is present
 * but unset, the switch statement should be completely bypassed.</p>
 */
class SwitchGuardUnsetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public SwitchGuardUnsetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/switchGuardUnset",
        "fuzz.runtime.switch.guardunset",
        "SwitchGuardUnset",
        List.of(new SymbolCountCheck("fuzz.runtime.switch.guardunset", 1)));
  }
}
