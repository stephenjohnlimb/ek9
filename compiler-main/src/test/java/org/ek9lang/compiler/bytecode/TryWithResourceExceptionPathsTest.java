package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for try-with-resources exception handling.
 * Tests resource cleanup under various exception scenarios.
 *
 * <p>Test cases (controlled by command-line argument):
 * <ul>
 *   <li>1. Single resource, exception in try block</li>
 *   <li>2. Single resource, normal path (no exception)</li>
 *   <li>3. Resource close() throws exception</li>
 *   <li>4. Multiple resources, exception in try block</li>
 *   <li>5. Multiple resources, first resource close() fails</li>
 *   <li>6. Multiple resources, second resource close() fails</li>
 *   <li>7. Exception in try + exception in close (suppressed)</li>
 * </ul>
 * </p>
 */
class TryWithResourceExceptionPathsTest extends AbstractExecutableBytecodeTest {
  public TryWithResourceExceptionPathsTest() {
    super("/examples/bytecodeGeneration/tryWithResourceExceptionPaths",
        "bytecode.test.resources",
        "TryWithResourceExceptionPaths",
        List.of(new SymbolCountCheck("bytecode.test.resources", 3)));  // 2 classes + 1 program
  }
}
