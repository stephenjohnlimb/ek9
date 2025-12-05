package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test JSON generation for a class with Boolean and Float fields.
 * Verifies that Boolean and Float values are correctly rendered as JSON primitives.
 */
class JsonBooleanFloatTest extends AbstractExecutableBytecodeTest {

  public JsonBooleanFloatTest() {
    super("/examples/bytecodeGeneration/jsonBooleanFloat",
        "bytecode.jsonBooleanFloat",
        "JsonBooleanFloatTest",
        List.of(new SymbolCountCheck("bytecode.jsonBooleanFloat", 2)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
