package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test JSON generation for a class containing another class as a field.
 * Verifies that nested objects produce correct nested JSON structure.
 */
class JsonNestedObjectTest extends AbstractExecutableBytecodeTest {

  public JsonNestedObjectTest() {
    super("/examples/bytecodeGeneration/jsonNestedObject",
        "bytecode.jsonNestedObject",
        "JsonNestedObjectTest",
        List.of(new SymbolCountCheck("bytecode.jsonNestedObject", 3)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
