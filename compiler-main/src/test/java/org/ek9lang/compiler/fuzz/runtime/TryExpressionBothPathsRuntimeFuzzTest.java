package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Try expression form with both success and exception paths.
 *
 * <p>Tests that try expression returns correct value from either path,
 * verifying both code paths produce expected results.</p>
 */
class TryExpressionBothPathsRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryExpressionBothPathsRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryExpressionBothPaths",
        "fuzz.runtime.try.expression.bothpaths",
        "TryExpressionBothPaths",
        List.of(new SymbolCountCheck("fuzz.runtime.try.expression.bothpaths", 3)));
  }
}
