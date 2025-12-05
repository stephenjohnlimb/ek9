package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test basic JSON functionality without synthetic operators.
 */
class JsonBasicTestTest extends AbstractExecutableBytecodeTest {

  public JsonBasicTestTest() {
    super("/examples/bytecodeGeneration/jsonBasicTest",
        "bytecode.test",
        "JsonBasicTest",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}
