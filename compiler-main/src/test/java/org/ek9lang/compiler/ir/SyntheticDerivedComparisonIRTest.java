package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for synthetic derived comparison operators on classes.
 *
 * <p>This verifies that DerivedComparisonGenerator correctly produces IR for
 * {@literal <}, {@literal <=}, {@literal >}, {@literal >=} operators that:</p>
 * <ul>
 *   <li>Call this._cmp(other) to get Integer comparison result</li>
 *   <li>Check if result is set, return unset Boolean if not</li>
 *   <li>Compare result to 0 using Integer._lt/_lte/_gt/_gte</li>
 *   <li>Return the Boolean comparison result</li>
 * </ul>
 *
 * <p>These operators delegate to _cmp, leveraging its existing tri-state semantics.</p>
 */
class SyntheticDerivedComparisonIRTest extends AbstractIRGenerationTest {

  public SyntheticDerivedComparisonIRTest() {
    super("/examples/irGeneration/synthetic/classDerivedComparison",
        List.of(new SymbolCountCheck("synthetic.classDerivedComparison", 1)
        ), false, false, false);
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
