package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for a simple HelloWorld program.
 * This test validates the @BYTE_CODE directive infrastructure by compiling
 * a minimal EK9 program and checking generated JVM bytecode.
 *<p>
 * IMPORTANT: Each bytecode test MUST have its own directory to enable parallel execution
 * without .ek9 directory contention (see AbstractBytecodeGenerationTest javadoc).
 * </p>
 */
class HelloWorldTest extends AbstractBytecodeGenerationTest {

  public HelloWorldTest() {
    //Each bytecode test gets its own directory for parallel execution safety
    //Disable showBytecode until getPackageModuleName() issue is resolved
    //Module name: bytecode.test, expected symbol count: 1 (the program)
    super("/examples/bytecodeGeneration/helloWorld",
        List.of(new SymbolCountCheck("bytecode.test", 1)),
        false, false, false);
  }
}
