package org.ek9lang.compiler.phase5;

import java.util.List;

/**
 * Designed to demonstrate correct loop statements and loop expressions.
 * With one deliberate failure to just check all the other examples compile correctly.
 */
class VariousLoopsTest extends BadPreIRChecksTest {

  public VariousLoopsTest() {
    super("/examples/parseButFailCompile/loopStatementsExpressions",
        List.of("just.various.loops"));
  }
}
