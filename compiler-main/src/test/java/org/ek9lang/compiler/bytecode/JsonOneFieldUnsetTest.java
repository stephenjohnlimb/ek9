package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test JSON generation for a class with one field unset.
 * Verifies that unset fields are excluded from JSON output.
 */
class JsonOneFieldUnsetTest extends AbstractExecutableBytecodeTest {

  public JsonOneFieldUnsetTest() {
    super("/examples/bytecodeGeneration/jsonOneFieldUnset",
        "bytecode.jsonOneFieldUnset",
        "JsonOneFieldUnsetTest",
        List.of(new SymbolCountCheck("bytecode.jsonOneFieldUnset", 2)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
