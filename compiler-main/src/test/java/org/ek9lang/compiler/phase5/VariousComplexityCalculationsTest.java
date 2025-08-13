package org.ek9lang.compiler.phase5;

import java.util.List;

/**
 * Designed to demonstrate correct loop statements and loop expressions.
 * With one deliberate failure to just check all the other examples compile correctly.
 */
class VariousComplexityCalculationsTest extends BadPreIRChecksTest {

  public VariousComplexityCalculationsTest() {
    super("/examples/parseAndCompile/complexity",
        List.of("simple.ifcomplexity",
            "simple.switchcomplexity",
            "simple.trycatchcomplexity",
            "simple.forloopcomplexity",
            "simple.whileloopcomplexity",
            "simple.streamcomplexity",
            "simple.unsetassignmentcomplexity",
            "argument.complexity",
            "excessive.code.block.complexity",
            "excessive.classdefinition.complexity"));
  }
}
