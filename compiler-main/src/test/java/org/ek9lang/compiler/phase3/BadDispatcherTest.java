package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad examples of dispatcher methods.
 */
class BadDispatcherTest extends BadFullResolutionTest {

  public BadDispatcherTest() {
    super("/examples/parseButFailCompile/badDispatchers",
        List.of("bad.dispatchermethods"));
  }

}
