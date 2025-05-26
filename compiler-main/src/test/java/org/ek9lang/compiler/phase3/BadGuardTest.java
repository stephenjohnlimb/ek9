package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad usage of guard use.
 */
class BadGuardTest extends BadFullResolutionTest {

  public BadGuardTest() {
    super("/examples/parseButFailCompile/badGuards",
        List.of("some.bad.ifguards",
            "some.bad.whileguards",
            "some.bad.dowhileguards",
            "some.bad.forloopguards",
            "some.bad.forrangeguards",
            "some.bad.trycatchfinallyguards",
            "some.bad.switchguards")
    );
  }
}
