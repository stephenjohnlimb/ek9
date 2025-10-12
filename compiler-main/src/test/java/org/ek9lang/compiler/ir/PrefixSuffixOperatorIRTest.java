package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focused on testing IR generation for prefix/suffix operator expressions.
 * Tests prefix (#<) and suffix (#>) operators with Dimension type.
 */
class PrefixSuffixOperatorIRTest extends AbstractIRGenerationTest {

  public PrefixSuffixOperatorIRTest() {
    super("/examples/irGeneration/operatorUse/prefixSuffix",
        List.of(
            new SymbolCountCheck(1, "prefix.test", 1),
            new SymbolCountCheck(1, "suffix.test", 1)
        ), false, false, false);
  }

}