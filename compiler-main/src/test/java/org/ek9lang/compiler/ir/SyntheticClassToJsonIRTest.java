package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for synthetic _json ($$) operator on classes.
 * This verifies that SyntheticOperatorGenerator produces correct IR.
 */
class SyntheticClassToJsonIRTest extends AbstractIRGenerationTest {

  public SyntheticClassToJsonIRTest() {
    super("/examples/irGeneration/synthetic/classToJson",
        List.of(new SymbolCountCheck("synthetic.classToJson", 1)
        ), false, false, true); // showIR=true to see generated IR
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
