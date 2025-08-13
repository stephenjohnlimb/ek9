package org.ek9lang.compiler.phase2;

import java.util.List;

/**
 * Just tests duplicate methods/operators on constructs.
 */
class BadDuplicateOperationsTest extends BadExplicitTypeDefinitionTest {

  public BadDuplicateOperationsTest() {
    super("/examples/parseButFailCompile/phase2/badDuplicateOperations",
        List.of("bad.duplicate.classmethods",
            "bad.duplicate.traitmethods",
            "bad.duplicate.recordmethods",
            "bad.duplicate.servicemethods",
            "bad.duplicate.componentmethods",
            "bad.duplicate.recordoperators",
            "bad.name.collisions1",
            "bad.name.collisions2"));
  }
}
