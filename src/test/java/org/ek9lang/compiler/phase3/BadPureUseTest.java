package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad pure usage.
 */
class BadPureUseTest extends BadFullResolutionTest {

  public BadPureUseTest() {
    super("/examples/parseButFailCompile/badPureUse",
        List.of("bad.pure.scenarios1",
            "bad.pure.scenarios2",
            "bad.pure.expressions",
            "bad.pure.declarations",
            "bad.pure.delegate.scenarios1",
            "bad.pure.delegate.scenarios2",
            "bad.pure.text.components"));
  }
}
