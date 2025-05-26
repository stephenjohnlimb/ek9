package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad class operators in assignments usage.
 */
class BadClassOperatorsInAssignmentTest extends BadSymbolDefinitionTest {

  public BadClassOperatorsInAssignmentTest() {
    super("/examples/parseButFailCompile/badClassAssignments",
        List.of("bad.classassignment.use"));
  }
}
