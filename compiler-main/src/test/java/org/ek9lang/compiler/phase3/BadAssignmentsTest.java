package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad assignments usage.
 */
class BadAssignmentsTest extends BadFullResolutionTest {

  public BadAssignmentsTest() {
    super("/examples/parseButFailCompile/badAssignments",
        List.of("bad.assignment.use", "bad.assignments.classes",
            "bad.coalescing.assignments", "bad.guardassignment.use"), false, true);
  }

}
