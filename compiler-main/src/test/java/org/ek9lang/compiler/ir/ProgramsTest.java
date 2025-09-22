package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focussed on testing IR with programs.
 */
class ProgramsTest extends AbstractIRGenerationTest {

  public ProgramsTest() {
    super("/examples/irGeneration/programs",
        List.of(new SymbolCountCheck("introduction1", 2),
            new SymbolCountCheck("introduction2", 3)
        ), false, false, false);
  }

}
