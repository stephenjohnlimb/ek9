package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focused on testing IR generation for advanced operator expressions.
 */
class AdvancedOperatorTest extends AbstractIRGenerationTest {

  public AdvancedOperatorTest() {
    super("/examples/irGeneration/operatorUse/advanced",
        List.of(
            new SymbolCountCheck(1, "replace.test", 1),
            new SymbolCountCheck(1, "merge.test", 1)
        ), false, false, false);
  }

}