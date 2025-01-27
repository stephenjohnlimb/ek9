package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Tests simple coalescing operations, some good and some bad, detecting errors.
 */
class BadBasicCoalescingUsesTest extends BadFullResolutionTest {

  public BadBasicCoalescingUsesTest() {
    super("/examples/parseButFailCompile/basicCoalescing",
        List.of("coalescing.elvis"));
  }

}
