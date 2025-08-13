package org.ek9lang.compiler.phase5;

import java.util.List;

/**
 * Focus on use of the EK9 Result type.
 * Both valid and invalid use.
 */
class ResultAndOptionalAccessTest extends BadPreIRChecksTest {

  public ResultAndOptionalAccessTest() {
    super("/examples/parseButFailCompile/phase5/badSpecialGenericsUse",
        List.of("error.on.result.access", "error.on.optional.access"));
  }
}
