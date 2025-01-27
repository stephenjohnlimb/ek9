package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad dynamic functions with dynamic variable capture usage.
 */
class BadDynamicFunctionsTest extends BadSymbolDefinitionTest {

  public BadDynamicFunctionsTest() {
    super("/examples/parseButFailCompile/badDynamicFunctions",
        List.of("bad.function.use"));
  }
}
