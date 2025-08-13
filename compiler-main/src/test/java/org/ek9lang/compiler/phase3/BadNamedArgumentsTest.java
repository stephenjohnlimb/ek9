package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad named arguments on functions and methods.
 */
class BadNamedArgumentsTest extends BadFullResolutionTest {

  public BadNamedArgumentsTest() {
    super("/examples/parseButFailCompile/phase3/badNamedArguments",
        List.of("bad.named.arguments.examples"));
  }

}
