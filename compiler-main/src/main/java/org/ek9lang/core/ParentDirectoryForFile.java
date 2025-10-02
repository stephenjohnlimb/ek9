package org.ek9lang.core;

import java.io.File;
import java.util.function.Function;

/**
 * Gets the immediate parent directory of the file.
 * This will return '.' if the names file is just 'helloWorld.ek9' for example.
 */
public class ParentDirectoryForFile implements Function<File, String> {
  @Override
  public String apply(final File file) {
    final var parentDir = file.getParent();
    if (parentDir == null || parentDir.isEmpty()) {
      return ".";
    }
    return parentDir;
  }
}
