package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Temporary test to examine IR generation for custom exception classes.
 * This validates that CustomExceptionA and CustomExceptionB are correctly
 * generated with fields, constructors, and super calls before implementing
 * bytecode generation.
 */
class ThrowCatchExceptionSubtypesIRTest extends AbstractIRGenerationTest {

  public ThrowCatchExceptionSubtypesIRTest() {
    // Module name: bytecode.test
    // Expected symbols: 1 program + 2 exception classes = 3
    // showIR = true to see the IR output
    super("/examples/bytecodeGeneration/throwCatchExceptionSubtypes",
        List.of(new SymbolCountCheck("bytecode.test", 3)),
        false, false, true);  // verbose=false, muteErrors=false, showIR=true
  }
}
