package org.ek9lang.compiler.symbol.support;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
import java.util.function.UnaryOperator;

public class PathToSourceFromName implements UnaryOperator<String> {
  @Override
  public String apply(String resourceName) {
    URL url = PathToSourceFromName.class.getResource(resourceName);
    assertNotNull(url, "Expecting URL to be available.");
    return url.getPath();
  }
}
