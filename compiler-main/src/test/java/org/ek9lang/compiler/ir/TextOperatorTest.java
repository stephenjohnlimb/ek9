package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focused on testing IR generation for text/string operator expressions.
 * Tests contains and matches operators which work with String content and patterns.
 */
class TextOperatorTest extends AbstractIRGenerationTest {

  public TextOperatorTest() {
    super("/examples/irGeneration/operatorUse/text",
        List.of(
            new SymbolCountCheck(1, "contains.test", 1),
            new SymbolCountCheck(1, "matches.test", 1)
        ), false, false, false);
  }

}