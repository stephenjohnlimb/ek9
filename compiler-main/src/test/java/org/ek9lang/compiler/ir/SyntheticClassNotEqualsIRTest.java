package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for synthetic _neq ({@literal <>}) operator on classes.
 *
 * <p>This verifies that SyntheticOperatorGenerator correctly produces IR that:</p>
 * <ul>
 *   <li>Delegates to _eq for the equality check</li>
 *   <li>Negates the result (true becomes false, false becomes true)</li>
 *   <li>Properly handles unset values (returns unset if eq returns unset)</li>
 * </ul>
 */
class SyntheticClassNotEqualsIRTest extends AbstractIRGenerationTest {

  public SyntheticClassNotEqualsIRTest() {
    super("/examples/irGeneration/synthetic/classNotEquals",
        List.of(new SymbolCountCheck("synthetic.classNotEquals", 1)
        ), false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
