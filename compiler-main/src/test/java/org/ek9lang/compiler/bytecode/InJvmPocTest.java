package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Proof-of-concept test for in-JVM bytecode execution with file-based test discovery.
 * Demonstrates that compiled EK9 programs can be loaded and executed within
 * the JUnit test JVM, avoiding the overhead of spawning external JVM processes.
 *
 * <h2>Test Cases</h2>
 * <ul>
 *   <li>Case "hello": Input="hello", Expected output="Hello, hello!\nPOC Complete\n"</li>
 *   <li>Case "world": Input="world", Expected output="Hello, world!\nPOC Complete\n"</li>
 * </ul>
 */
class InJvmPocTest extends AbstractExecutableBytecodeTest {

  public InJvmPocTest() {
    super("/examples/bytecodeGeneration/inJvmPoc",
        "bytecode.test.injvmpoc",
        "InJvmPoc",
        List.of(new SymbolCountCheck("bytecode.test.injvmpoc", 1)));
  }
}
