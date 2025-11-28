package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for synthetic _eq (==) operator on classes with inheritance.
 * This verifies that SyntheticOperatorGenerator correctly calls super._eq().
 */
class SyntheticClassInheritanceEqualsIRTest extends AbstractIRGenerationTest {

  public SyntheticClassInheritanceEqualsIRTest() {
    super("/examples/irGeneration/synthetic/classInheritanceEquals",
        List.of(new SymbolCountCheck("synthetic.classInheritanceEquals", 2)
        ), false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
