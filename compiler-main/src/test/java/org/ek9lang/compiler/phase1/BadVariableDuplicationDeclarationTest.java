package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad use of duplicated variables.
 */
class BadVariableDuplicationDeclarationTest extends BadSymbolDefinitionTest {

  public BadVariableDuplicationDeclarationTest() {
    super("/examples/parseButFailCompile/phase1/existingVariables",
        List.of("bad.variable.duplications"));
  }
}
