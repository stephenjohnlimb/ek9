package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad class modifier usage.
 */
class BadVariableOnlyDeclarationTest extends BadSymbolDefinitionTest {


  public BadVariableOnlyDeclarationTest() {
    super("/examples/parseButFailCompile/badVariableOnlyDeclarations",
        List.of("bad.variableonly.use"));
  }
}
