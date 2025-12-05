package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test JSON generation for a class with multiple fields of different types.
 * Verifies that synthetic _json ($$) operator produces correct JSON with field order preserved.
 */
class JsonMultipleFieldsTest extends AbstractExecutableBytecodeTest {

  public JsonMultipleFieldsTest() {
    super("/examples/bytecodeGeneration/jsonMultipleFields",
        "bytecode.jsonMultipleFields",
        "JsonMultipleFieldsTest",
        List.of(new SymbolCountCheck("bytecode.jsonMultipleFields", 2)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
