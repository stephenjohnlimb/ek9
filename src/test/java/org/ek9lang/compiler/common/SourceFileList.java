package org.ek9lang.compiler.common;

import java.io.File;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.core.Glob;
import org.ek9lang.core.OsSupport;

/**
 * Get a list of source files from a resource directory.
 */
public final class SourceFileList implements Function<String, List<File>> {

  private final ActualPathFromResourcesDirectory actualPathFromResourcesDirectory =
      new ActualPathFromResourcesDirectory();
  private final OsSupport os = new OsSupport();

  @Override
  public List<File> apply(String fromDirectory) {

    final var rootDirectoryForTest = actualPathFromResourcesDirectory.apply(fromDirectory);
    File examples = new File(rootDirectoryForTest);
    Glob ek9 = new Glob("**.ek9");

    return os.getFilesRecursivelyFrom(examples, ek9);
  }
}
