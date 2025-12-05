package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test JSON generation for a class with a single Integer field.
 * Verifies that synthetic _json ($$) operator produces correct JSON: {"age":25}
 */
class JsonSingleIntegerTest extends AbstractExecutableBytecodeTest {

  public JsonSingleIntegerTest() {
    super("/examples/bytecodeGeneration/jsonSingleInteger",
        "bytecode.jsonSingleInteger",
        "JsonSingleIntegerTest",
        List.of(new SymbolCountCheck("bytecode.jsonSingleInteger", 2)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
