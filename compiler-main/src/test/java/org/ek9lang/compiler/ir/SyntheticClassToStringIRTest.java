package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for synthetic _string ($) operator on classes.
 * This verifies that SyntheticOperatorGenerator produces correct IR.
 */
class SyntheticClassToStringIRTest extends AbstractIRGenerationTest {

  public SyntheticClassToStringIRTest() {
    super("/examples/irGeneration/synthetic/classToString",
        List.of(new SymbolCountCheck("synthetic.classToString", 1)
        ), false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
