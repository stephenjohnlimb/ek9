package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests duplicated symbols but in different files.
 * For this test we don't use @directives -cause the multi-thread nature of N files means you cannot
 * guarantee the order of failure.
 */
class BadDuplicatedSymbolsDifferentFilesTest extends BadSymbolDefinitionTest {

  public BadDuplicatedSymbolsDifferentFilesTest() {
    super("/examples/parseButFailCompile/duplicatedInDifferentFiles",
        List.of("duplications"));
  }
}
