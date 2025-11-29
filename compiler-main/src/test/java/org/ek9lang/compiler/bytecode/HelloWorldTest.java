package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for a simple HelloWorld program.
 * This test validates the complete pipeline by compiling and executing
 * a minimal EK9 program and verifying its output.
 *<p>
 * IMPORTANT: Each bytecode test MUST have its own directory to enable parallel execution
 * without .ek9 directory contention (see AbstractBytecodeGenerationTest javadoc).
 * </p>
 */
class HelloWorldTest extends AbstractExecutableBytecodeTest {

  public HelloWorldTest() {
    //Each bytecode test gets its own directory for parallel execution safety
    //Module name: bytecode.test, program name: HelloWorld, expected symbol count: 1
    super("/examples/bytecodeGeneration/helloWorld",
        "bytecode.test",
        "HelloWorld",
        List.of(new SymbolCountCheck("bytecode.test", 1)));
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return true;  // Match @BYTECODE directive which includes debug sections
  }
}
