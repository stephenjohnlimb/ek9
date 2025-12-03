package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for synthetic _hashcode (#?) operator on classes.
 * This verifies that SyntheticOperatorGenerator produces correct IR.
 */
class SyntheticClassHashCodeIRTest extends AbstractIRGenerationTest {

  public SyntheticClassHashCodeIRTest() {
    super("/examples/irGeneration/synthetic/classHashCode",
        List.of(new SymbolCountCheck("synthetic.classHashCode", 1)
        ), false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
