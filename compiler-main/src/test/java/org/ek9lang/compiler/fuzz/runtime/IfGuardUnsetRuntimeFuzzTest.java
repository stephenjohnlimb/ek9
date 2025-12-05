package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: IF statement with unset guard variable.
 *
 * <p>Tests that the IF body is correctly skipped when the guard variable
 * is unset (tri-state semantics). The guard check uses _isSet() at runtime.</p>
 *
 * <p>This verifies EK9's tri-state semantics: when a guard variable is present
 * but unset, the conditional body should not execute.</p>
 */
class IfGuardUnsetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public IfGuardUnsetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/ifGuardUnset",
        "fuzz.runtime.if.guardunset",
        "IfGuardUnset",
        List.of(new SymbolCountCheck("fuzz.runtime.if.guardunset", 1)));
  }
}
