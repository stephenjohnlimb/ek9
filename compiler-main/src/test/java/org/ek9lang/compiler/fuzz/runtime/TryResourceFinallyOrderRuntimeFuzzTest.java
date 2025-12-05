package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Establish order of close() vs explicit finally.
 *
 * <p>Tests the order of operations when try-with-resources has BOTH
 * a resource (with implicit close) AND an explicit finally block.</p>
 *
 * <p>Key finding: close() runs BEFORE explicit finally, matching Java semantics.
 * Order is: Body → close() → Explicit finally</p>
 *
 * <p>This establishes baseline semantics before testing exception scenarios.</p>
 */
class TryResourceFinallyOrderRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryResourceFinallyOrderRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryResourceFinallyOrder",
        "fuzz.runtime.resource.finallyorder",
        "TryResourceFinallyOrder",
        List.of(new SymbolCountCheck("fuzz.runtime.resource.finallyorder", 2)));  // 1 class + 1 program
  }
}
