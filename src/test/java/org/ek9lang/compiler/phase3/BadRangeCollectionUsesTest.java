package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Tests in range and in collection operations, some good and some bad, detecting errors.
 */
class BadRangeCollectionUsesTest extends BadFullResolutionTest {

  public BadRangeCollectionUsesTest() {
    super("/examples/parseButFailCompile/badInExpressions",
        List.of("bad.range.collections"));
  }

}
