package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad switch usage with enumerations.
 */
class BadSwitchesTest extends BadFullResolutionTest {


  public BadSwitchesTest() {
    super("/examples/parseButFailCompile/badSwitches",
        List.of("bad.switches.enums", "bad.switches.use"));
  }

}
