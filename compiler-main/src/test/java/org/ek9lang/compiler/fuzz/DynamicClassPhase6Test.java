package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

class DynamicClassPhase6Test extends FuzzTestBase {
  public DynamicClassPhase6Test() {
    super("dynamicClassFunction/phase6", CompilationPhase.FULL_RESOLUTION, false);
  }

  @Test
  void test() {
    assertTrue(runTests() != 0);
  }
}
