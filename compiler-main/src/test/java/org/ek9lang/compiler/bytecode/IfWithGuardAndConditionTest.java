package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for if statement with guard and nested condition check.
 */
class IfWithGuardAndConditionTest extends AbstractExecutableBytecodeTest {
  public IfWithGuardAndConditionTest() {
    super("/examples/bytecodeGeneration/ifWithGuardAndCondition",
        "bytecode.test",
        "IfWithGuardAndCondition",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}
