package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Bug #3 pattern with resource close() exception.
 *
 * <p>Validates that Bug #3 fix applies to resource close() exceptions,
 * not just explicit throw statements.</p>
 *
 * <p>Pattern: Inner try-with-resources (close throws), outer catch + finally.
 * This is exactly Bug #3 pattern but with implicit close() exception.</p>
 *
 * <p>Expected: close() exception caught by outer catch, outer finally runs,
 * program continues to "Done".</p>
 */
class TryResourceCloseThrowsBug3RuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryResourceCloseThrowsBug3RuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryResourceCloseThrowsBug3",
        "fuzz.runtime.resource.closethrowsbug3",
        "TryResourceCloseThrowsBug3",
        List.of(new SymbolCountCheck("fuzz.runtime.resource.closethrowsbug3", 2)));  // 1 class + 1 program
  }
}
