package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad program usage.
 */
class BadProgramsTest extends BadSymbolDefinitionTest {


  public BadProgramsTest() {
    super("/examples/parseButFailCompile/badPrograms",
        List.of("bad.program.return", "bad.argument.parameters"));
  }
}
