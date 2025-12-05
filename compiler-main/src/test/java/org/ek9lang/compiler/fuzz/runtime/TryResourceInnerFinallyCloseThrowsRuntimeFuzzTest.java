package org.ek9lang.compiler.fuzz.runtime;

import java.util.List;
import org.ek9lang.compiler.bytecode.AbstractExecutableBytecodeTest;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Runtime Fuzz Test: Full complexity - inner resource + explicit finally + close() throws.
 *
 * <p>This is the most complex case combining:
 * <ul>
 *   <li>Inner try-with-resources (close throws)</li>
 *   <li>Inner explicit finally block</li>
 *   <li>Outer catch + finally (Bug #3 pattern)</li>
 * </ul>
 * </p>
 *
 * <p>Validates Bug #4 fix: User's explicit finally code runs BEFORE close(),
 * ensuring user's cleanup code is guaranteed to execute even when close() throws.</p>
 *
 * <p>Expected order: body → user's finally → close() (throws) → outer catch → outer finally → Done</p>
 */
class TryResourceInnerFinallyCloseThrowsRuntimeFuzzTest extends AbstractExecutableBytecodeTest {

  public TryResourceInnerFinallyCloseThrowsRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/tryResourceInnerFinallyCloseThrows",
        "fuzz.runtime.resource.innerfinallyclose",
        "TryResourceInnerFinallyCloseThrows",
        List.of(new SymbolCountCheck("fuzz.runtime.resource.innerfinallyclose", 2)));  // 1 class + 1 program
  }
}
