package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test JSON generation for a class with all fields unset.
 * Verifies that an empty JSON object is produced when no fields are set.
 */
class JsonAllFieldsUnsetTest extends AbstractExecutableBytecodeTest {

  public JsonAllFieldsUnsetTest() {
    super("/examples/bytecodeGeneration/jsonAllFieldsUnset",
        "bytecode.jsonAllFieldsUnset",
        "JsonAllFieldsUnsetTest",
        List.of(new SymbolCountCheck("bytecode.jsonAllFieldsUnset", 2)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
