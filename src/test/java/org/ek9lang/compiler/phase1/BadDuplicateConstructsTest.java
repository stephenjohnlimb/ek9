package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests duplicate constructs.
 */
class BadDuplicateConstructsTest extends BadSymbolDefinitionTest {

  public BadDuplicateConstructsTest() {
    super("/examples/parseButFailCompile/badDuplicateConstructs",
        List.of("bad.duplicate.constructs"));
  }
}