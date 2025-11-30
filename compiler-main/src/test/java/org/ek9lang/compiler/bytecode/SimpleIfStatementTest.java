package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for simple if statement (without else).
 * This test validates that conditional branching is correctly generated
 * in JVM bytecode for basic if statements.
 * <p>
 * Tests:
 * - If condition evaluation
 * - Conditional branching (ifeq/ifne)
 * - Body execution when condition is true
 * - Skipping body when condition is false
 * - Program flow continuation after if
 * </p>
 */
class SimpleIfStatementTest extends AbstractExecutableBytecodeTest {

  public SimpleIfStatementTest() {
    super("/examples/bytecodeGeneration/simpleIfStatement",
        "bytecode.test",
        "SimpleIfStatement",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }
}