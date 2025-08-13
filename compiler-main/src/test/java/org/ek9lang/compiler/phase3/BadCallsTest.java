package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad calls.
 */
class BadCallsTest extends BadFullResolutionTest {

  public BadCallsTest() {
    super("/examples/parseButFailCompile/phase3/badCalls",
        List.of("bad.functioncall.examples1",
            "bad.enumeratedtypecall.examples1",
            "bad.constrainedtypecall.examples1",
            "bad.recordcalls.examples1",
            "bad.classcalls.examples1",
            "bad.componentcalls.examples1",
            "bad.textcalls.examples1",
            "bad.abstractcalls.examples1",
            "bad.abstractuse.examples1"));
  }

}
