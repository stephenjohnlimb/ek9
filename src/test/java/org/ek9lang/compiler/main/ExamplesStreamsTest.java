package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Just test streams all compile.
 */
class ExamplesStreamsTest extends PhasesTest {

  public ExamplesStreamsTest() {
    super("/examples/streams", false, false);
  }

  @Test
  void testPhaseDevelopment() {
    //has issues with Generics specifically Optional type still being T rather than JSON.
    //TODO fix up example code and move to PRE_IR_CHECKS
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("com.customer.justcat", 5).test(program);
    new SymbolCountCheck("com.customer.justparagraphs", 3).test(program);
    new SymbolCountCheck("com.customer.justmoney", 1).test(program);
    new SymbolCountCheck("ekopen.io.file.examples", 1).test(program);
    new SymbolCountCheck("com.customer.streams.collectas", 2).test(program);
    new SymbolCountCheck("com.customer.streams.splitter", 9).test(program);
  }
}
