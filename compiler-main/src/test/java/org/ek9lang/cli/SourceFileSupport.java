package org.ek9lang.cli;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.OsSupport;

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
    File fileToCopy = new File(getPath(fromRelativeTestUrl, ek9SourceFileName));

    //Now we can put a dummy source file in the simulated/test cwd and then try and process it.
    File cwd = new File(osSupport.getCurrentWorkingDirectory());
    assertTrue(cwd.exists());
    return copyToDirectory(fileToCopy, cwd, ek9SourceFileName);
  }

  public File copyFileToTestDirectoryUnderCWD(String fromRelativeTestUrl,
                                              String ek9SourceFileName,
                                              String directoryNameUnderCWD) {
    File fileToCopy = new File(getPath(fromRelativeTestUrl, ek9SourceFileName));

    //Now we can put a dummy source file in the simulated/test cwd and then try and process it.
    File cwd = new File(osSupport.getCurrentWorkingDirectory());
    assertTrue(cwd.exists());
    File directory = !directoryNameUnderCWD.equals(".")
        ? new File(cwd, directoryNameUnderCWD)
        : cwd;
    osSupport.makeDirectoryIfNotExists(directory);
    assertTrue(directory.exists());
    return copyToDirectory(fileToCopy, directory, ek9SourceFileName);
  }

  private File copyToDirectory(File toCopy, File directory, String fileName) {
    File aSourceFile = new File(directory, fileName);
    if (!aSourceFile.exists()) {
      assertTrue(fileHandling.copy(new File(toCopy.getParent()), directory, fileName));
      assertTrue(aSourceFile.exists());
    }

    return aSourceFile;
  }
}
