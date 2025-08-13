package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad stream combinations. There are lots!
 */
class BadStreamsTest extends BadFullResolutionTest {

  public BadStreamsTest() {
    super("/examples/parseButFailCompile/phase3/badStreams",
        List.of("bad.streams1", "bad.streams2", "bad.streams3", "bad.streams4",
            "bad.streams5", "bad.streams6", "bad.streams7", "bad.streams8",
            "bad.streams9", "bad.streams10", "bad.streams11", "bad.streams12",
            "bad.streams13", "bad.streams14"));
  }

}
