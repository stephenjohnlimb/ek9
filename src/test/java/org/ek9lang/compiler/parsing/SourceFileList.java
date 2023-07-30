package org.ek9lang.compiler.parsing;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.core.Glob;
import org.ek9lang.core.OsSupport;

/**
 * Get a list of source files from a resource directory.
 */
public class SourceFileList implements Function<String, List<File>> {

  @Override
  public List<File> apply(String fromDirectory) {
    OsSupport os = new OsSupport();
    URL rootDirectoryForTest = this.getClass().getResource(fromDirectory);
    assertNotNull(rootDirectoryForTest);
    File examples = new File(rootDirectoryForTest.getPath());
    Glob ek9 = new Glob("**.ek9");

    return os.getFilesRecursivelyFrom(examples, ek9);
  }
}
