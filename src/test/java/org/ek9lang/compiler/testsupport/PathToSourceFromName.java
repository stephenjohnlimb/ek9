package org.ek9lang.compiler.testsupport;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
import java.util.function.UnaryOperator;

public class PathToSourceFromName implements UnaryOperator<String> {
  @Override
  public String apply(String resourceName) {
    URL helloWorld = PathToSourceFromName.class.getResource(resourceName);

    assertNotNull(helloWorld, "Expecting URL to be available.");
    return helloWorld.getPath();
  }
}
