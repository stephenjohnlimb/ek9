package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Try guard unset skips entire block including finally.
 *
 * <p>Tests EK9 tri-state semantics: when try guard is unset, the entire
 * try/catch/finally block is skipped - not just the try body.</p>
 *
 * <p>This is different from traditional languages where finally always runs.</p>
 */
class TryGuardUnsetSkipsAllRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryGuardUnsetSkipsAllRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryGuardUnsetSkipsAll",
        "fuzz.runtime.try.guard.unsetskipsall",
        "TryGuardUnsetSkipsAll",
        List.of(new SymbolCountCheck("fuzz.runtime.try.guard.unsetskipsall", 1)));
  }
}
