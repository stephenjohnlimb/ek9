package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test JSON generation for a class containing a List of Strings.
 * Verifies that List fields are correctly rendered as JSON arrays.
 */
class JsonListFieldTest extends AbstractExecutableBytecodeTest {

  public JsonListFieldTest() {
    super("/examples/bytecodeGeneration/jsonListField",
        "bytecode.jsonListField",
        "JsonListFieldTest",
        List.of(new SymbolCountCheck("bytecode.jsonListField", 2)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
