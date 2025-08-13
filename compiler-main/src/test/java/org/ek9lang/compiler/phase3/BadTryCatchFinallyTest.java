package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad usage of try catch finally.
 */
class BadTryCatchFinallyTest extends BadFullResolutionTest {

  public BadTryCatchFinallyTest() {
    super("/examples/parseButFailCompile/phase3/badTryCatchFinally",
        List.of("bad.trycatchfinally.example"));
  }

}
