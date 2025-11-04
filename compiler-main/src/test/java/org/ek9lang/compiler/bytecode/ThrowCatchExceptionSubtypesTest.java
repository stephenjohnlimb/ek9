package org.ek9lang.compiler.bytecode;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test bytecode generation for exception subtype matching and polymorphism.
 * This test validates that exception type checking works correctly at bytecode level:
 * <p>
 * Tests:
 * - Exact type matching (throw CustomExceptionA, catch CustomExceptionA)
 * - Polymorphic matching (throw subtype, catch supertype)
 * - Type safety (throw CustomExceptionA, catch CustomExceptionB - should NOT match)
 * - Exception table with correct type descriptors
 * </p>
 * <p>
 * Critical for type safety: sibling exception types should NOT match each other.
 * Only supertype or exact type should match.
 * </p>
 */
class ThrowCatchExceptionSubtypesTest extends AbstractBytecodeGenerationTest {

  public ThrowCatchExceptionSubtypesTest() {
    // Module name: bytecode.test, expected symbols: 1 program + 2 exception classes = 3
    super("/examples/bytecodeGeneration/throwCatchExceptionSubtypes",
        List.of(new SymbolCountCheck("bytecode.test", 3)),
        false, false, false);
  }

  /**
   * Disable debug instrumentation for minimal bytecode output.
   */
  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }
}
