package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test JSON generation for inheritance scenario.
 * Verifies that both parent and child class fields are included in JSON output.
 */
class JsonInheritanceTest extends AbstractExecutableBytecodeTest {

  public JsonInheritanceTest() {
    super("/examples/bytecodeGeneration/jsonInheritance",
        "bytecode.jsonInheritance",
        "JsonInheritanceTest",
        List.of(new SymbolCountCheck("bytecode.jsonInheritance", 3)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
