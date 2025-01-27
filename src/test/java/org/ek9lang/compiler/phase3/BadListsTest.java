package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad usage of lists.
 */
class BadListsTest extends BadFullResolutionTest {

  public BadListsTest() {
    super("/examples/parseButFailCompile/badLists",
        List.of("bad.lists.only"));
  }

}
