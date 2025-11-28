package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for synthetic _eq (==) operator on classes.
 * This verifies that SyntheticOperatorGenerator produces correct IR.
 */
class SyntheticClassEqualsIRTest extends AbstractIRGenerationTest {

  public SyntheticClassEqualsIRTest() {
    super("/examples/irGeneration/synthetic/classEquals",
        List.of(new SymbolCountCheck("synthetic.classEquals", 1)
        ), false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
