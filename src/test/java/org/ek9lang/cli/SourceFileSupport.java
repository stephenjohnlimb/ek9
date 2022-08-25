package org.ek9lang.cli;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import org.ek9lang.core.utils.FileHandling;
import org.ek9lang.core.utils.OsSupport;

public final class SourceFileSupport {
  private final OsSupport osSupport;
  private final FileHandling fileHandling;

  public SourceFileSupport(FileHandling fileHandling, OsSupport osSupport) {
    this.fileHandling = fileHandling;
    this.osSupport = osSupport;
  }

  public String getPath(String fromRelativeTestUrl, String ek9SourceFileName) {
    URL fileForTest = this.getClass().getResource(fromRelativeTestUrl + ek9SourceFileName);
    assertNotNull(fileForTest);
    return fileForTest.getPath();
  }

  public File copyFileToTestCWD(String fromRelativeTestUrl, String ek9SourceFileName) {
    File example = new File(getPath(fromRelativeTestUrl, ek9SourceFileName));

    //Now we can put a dummy source file in the simulated/test cwd and then try and process it.
    File cwd = new File(osSupport.getCurrentWorkingDirectory());
    assertTrue(cwd.exists());
    File aSourceFile = new File(cwd, ek9SourceFileName);
    if (!aSourceFile.exists()) {
      assertTrue(fileHandling.copy(new File(example.getParent()), cwd, ek9SourceFileName));
      assertTrue(aSourceFile.exists());
    }

    return aSourceFile;
  }
}
