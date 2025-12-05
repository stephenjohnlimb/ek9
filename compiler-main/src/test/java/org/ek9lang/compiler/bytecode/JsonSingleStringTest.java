package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test JSON generation for a class with a single String field.
 * Verifies that synthetic _json ($$) operator produces correct JSON: {"name":"Alice"}
 */
class JsonSingleStringTest extends AbstractExecutableBytecodeTest {

  public JsonSingleStringTest() {
    super("/examples/bytecodeGeneration/jsonSingleString",
        "bytecode.jsonSingleString",
        "JsonSingleStringTest",
        List.of(new SymbolCountCheck("bytecode.jsonSingleString", 2)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
