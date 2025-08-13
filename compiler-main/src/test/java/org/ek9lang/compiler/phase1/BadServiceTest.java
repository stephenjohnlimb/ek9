package org.ek9lang.compiler.phase1;

import java.util.List;

/**
 * Just tests bad service definition usage.
 */
class BadServiceTest extends BadSymbolDefinitionTest {

  public BadServiceTest() {
    super("/examples/parseButFailCompile/phase1/badServiceDefinition",
        List.of("bad.services.use"));
  }
}
