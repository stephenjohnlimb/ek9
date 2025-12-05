package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Bug #3 Reproduction.
 *
 * <p>This test reproduces the exact pattern that triggers Bug #3:
 * Inner try-finally (no catch) throws to outer try-catch-finally.
 * The exception should be caught and absorbed, but Bug #3 causes
 * the exception to propagate AFTER the catch block executes.</p>
 *
 * <p>Pattern: try { try { THROW } finally } catch finally</p>
 *
 * <p>This test should FAIL until Bug #3 is fixed.</p>
 */
class TryBug3ReproductionRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryBug3ReproductionRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryBug3Reproduction",
        "fuzz.runtime.try.bug3.reproduction",
        "TryBug3Reproduction",
        List.of(new SymbolCountCheck("fuzz.runtime.try.bug3.reproduction", 1)));
  }
}
