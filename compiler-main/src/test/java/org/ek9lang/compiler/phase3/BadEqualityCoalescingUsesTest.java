package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Tests equality coalescing operations, some good and some bad, detecting errors.
 */
class BadEqualityCoalescingUsesTest extends BadFullResolutionTest {

  public BadEqualityCoalescingUsesTest() {
    super("/examples/parseButFailCompile/phase3/equalityCoalescing",
        List.of("equality.coalescing"));
  }

}
