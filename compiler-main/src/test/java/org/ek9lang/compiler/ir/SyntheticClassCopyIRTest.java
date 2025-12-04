package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for synthetic _copy (:=:) operator on classes.
 * This verifies that SyntheticOperatorGenerator produces correct IR.
 */
class SyntheticClassCopyIRTest extends AbstractIRGenerationTest {

  public SyntheticClassCopyIRTest() {
    super("/examples/irGeneration/synthetic/classCopy",
        List.of(new SymbolCountCheck("synthetic.classCopy", 1)
        ), false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
