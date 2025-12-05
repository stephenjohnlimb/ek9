package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Try expression form, exception path returns value.
 *
 * <p>Tests that when try throws and catch handles, the catch block's
 * return value is used as the expression result.</p>
 */
class TryExpressionCatchReturnsRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryExpressionCatchReturnsRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryExpressionCatchReturns",
        "fuzz.runtime.try.expression.catchreturns",
        "TryExpressionCatchReturns",
        List.of(new SymbolCountCheck("fuzz.runtime.try.expression.catchreturns", 1)));
  }
}
