package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for synthetic derived comparison operators.
 * This test validates that the default operators {@literal <}, {@literal <=},
 * {@literal >}, {@literal >=} generate correct JVM bytecode.
 * <p>
 * These operators delegate to the _cmp ({@literal <=>}) operator and
 * interpret the Integer result.
 * </p>
 * <p>
 * Tests:
 * - _lt method: returns true if _cmp result {@literal <} 0
 * - _lte method: returns true if _cmp result {@literal <=} 0
 * - _gt method: returns true if _cmp result {@literal >} 0
 * - _gte method: returns true if _cmp result {@literal >=} 0
 * - All methods properly handle unset values from _cmp
 * </p>
 * <p>
 * Expected bytecode patterns:
 * - Call this._cmp(other) to get comparison result
 * - Check if result is set, return unset Boolean if not
 * - Compare Integer result to 0 using _lt/_lte/_gt/_gte
 * - Return the Boolean comparison result
 * </p>
 */
class SyntheticDerivedComparisonTest extends AbstractExecutableBytecodeTest {

  public SyntheticDerivedComparisonTest() {
    super("/examples/bytecodeGeneration/syntheticDerivedComparison",
        "bytecode.syntheticDerivedComparison",
        "TestSyntheticDerivedComparison",
        List.of(new SymbolCountCheck("bytecode.syntheticDerivedComparison", 2)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
