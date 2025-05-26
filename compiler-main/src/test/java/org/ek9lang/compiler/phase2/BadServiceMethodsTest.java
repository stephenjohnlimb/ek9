package org.ek9lang.compiler.phase2;

import java.util.List;

/**
 * Just tests bad use of service methods.
 */
class BadServiceMethodsTest extends BadExplicitTypeDefinitionTest {

  public BadServiceMethodsTest() {
    super("/examples/parseButFailCompile/badServiceMethods",
        List.of("bad.servicemethod.returntypes", "bad.servicemethod.argumenttypes"));
  }
}
