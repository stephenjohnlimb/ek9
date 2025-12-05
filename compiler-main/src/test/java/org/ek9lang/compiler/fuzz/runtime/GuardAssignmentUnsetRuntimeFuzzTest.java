package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Guard assignment with unset value.
 *
 * <p>Tests that guard assignment ({@code <-}) with an unset source value
 * correctly prevents the guarded block from executing.</p>
 *
 * <p>When a function returns an unset value, the if-guard should skip the block.</p>
 */
class GuardAssignmentUnsetRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public GuardAssignmentUnsetRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/guardAssignmentUnset",
        "fuzz.runtime.guard.assignmentunset",
        "GuardAssignmentUnset",
        List.of(new SymbolCountCheck("fuzz.runtime.guard.assignmentunset", 1)));
  }
}
