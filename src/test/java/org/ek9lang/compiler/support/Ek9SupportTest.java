package org.ek9lang.compiler.support;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import org.ek9lang.cli.SourceFileSupport;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;
import org.junit.jupiter.api.Test;

class Ek9SupportTest {
  private final OsSupport osSupport = new OsSupport(true);
  private final FileHandling fileHandling = new FileHandling(osSupport);
  private final SourceFileSupport sourceFileSupport =
      new SourceFileSupport(fileHandling, osSupport);

  @Test
  void testEk9SupportPreparation() throws IOException {

    var fileName = sourceFileSupport.getPath("/examples/basics/",
        "HelloWorld.ek9");
    Ek9Support underTest = new Ek9Support(fileName);
    assertNotNull(underTest, "UnderTest was not created");
  }
}
