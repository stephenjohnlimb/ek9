package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad access to fields and methods.
 */
class BadAccessChecksTest extends BadFullResolutionTest {

  public BadAccessChecksTest() {
    super("/examples/parseButFailCompile/phase3/badAccessChecks",
        List.of("bad.callthisandsuper.classmethod.access1",
            "bad.classfield.access",
            "bad.classmethod.access1",
            "bad.classmethod.access2",
            "bad.classmethod.access3",
            "bad.functiondelegates.examples",
            "bad.higherfunctionandmethodcalls.examples",
            "bad.recordfield.access",
            "bad.delegate.name.clashes"),false, true);
  }

}
