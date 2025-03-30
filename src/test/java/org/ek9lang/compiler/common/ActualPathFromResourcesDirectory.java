package org.ek9lang.compiler.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
import java.util.function.UnaryOperator;

/**
 * Just gets the full actual path when accessed via resources (for tests).
 */
public class ActualPathFromResourcesDirectory implements UnaryOperator<String> {
  @Override
  public String apply(final String fromDirectory) {
    URL rootDirectoryForTest = this.getClass().getResource(fromDirectory);
    assertNotNull(rootDirectoryForTest);
    return rootDirectoryForTest.getPath();
  }
}
