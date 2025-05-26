package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad use of types.
 */
class BadUseOfTypeTest extends BadFullResolutionTest {


  public BadUseOfTypeTest() {
    super("/examples/parseButFailCompile/badTypeUse",
        List.of("bad.type.use"));
  }

}
