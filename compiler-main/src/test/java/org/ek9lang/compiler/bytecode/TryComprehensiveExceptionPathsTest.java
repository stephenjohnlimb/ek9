package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation and execution for comprehensive exception handling paths.
 * Tests try/catch/finally combinations with actual exception throwing.
 *
 * <p>Test cases (controlled by command-line argument):
 * <ul>
 *   <li>1. Try/catch with exception thrown and caught</li>
 *   <li>2. Try/catch normal path (no exception)</li>
 *   <li>3. Try/finally with exception propagating through finally</li>
 *   <li>4. Try/catch/finally with exception caught</li>
 *   <li>5. Try/catch/finally with exception NOT caught</li>
 *   <li>6. Exception thrown FROM finally block</li>
 * </ul>
 * </p>
 */
class TryComprehensiveExceptionPathsTest extends AbstractExecutableBytecodeTest {
  public TryComprehensiveExceptionPathsTest() {
    super("/examples/bytecodeGeneration/tryComprehensiveExceptionPaths",
        "bytecode.test.exceptions",
        "TryComprehensiveExceptionPaths",
        List.of(new SymbolCountCheck("bytecode.test.exceptions", 1)));
  }
}
