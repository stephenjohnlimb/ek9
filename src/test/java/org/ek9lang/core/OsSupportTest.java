package org.ek9lang.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.ek9lang.compiler.common.CompilerReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ResourceLock;

//Specific tests that manipulate files and specifics in ek9 must not run in parallel.
@Execution(SAME_THREAD)
@ResourceLock(value = "file_access", mode = READ_WRITE)
final class OsSupportTest {

  //If you actually want to see the reports then alter the reporter settings.
  private final CompilerReporter reporter = new CompilerReporter(false, true);

  OsSupport underTest = new OsSupport();

  @Test
  void testNumberOfProcessors() {
    int count = underTest.getNumberOfProcessors();
    assertTrue(count > 1);
  }

  @Test
  void checkFailToCreateDirectory() {
    assertFalse(underTest.isInStubMode());
    var file = new File("/not valid directory");
    assertThrows(CompilerException.class,
        () -> underTest.makeDirectoryIfNotExists(file));
  }

  @Test
  void checkDirectories() {
    String cwd = underTest.getCurrentWorkingDirectory();
    assertNotNull(cwd);

    String home = underTest.getUsersHomeDirectory();
    assertNotNull(home);

    String temp = underTest.getTempDirectory();
    assertNotNull(temp);
  }

  @Test
  void testRecursiveListing() {
    URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
    assertNotNull(rootDirectoryForTest);
    String testPath = rootDirectoryForTest.getPath();

    List<File> files = underTest.getFilesRecursivelyFrom(new File(testPath));
    assertNotNull(files);
    assertNotSame(0, files.size());
  }

  @Test
  void testDirectoriesInDirectory() {
    URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
    assertNotNull(rootDirectoryForTest);
    String testPath = rootDirectoryForTest.getPath();

    //Needs the correct directory structure in /forFileFindTests
    List<File> directories = underTest.getDirectoriesInDirectory(new File(testPath), "cooper");
    assertNotNull(directories);
    assertEquals(2, directories.size());
  }

  @Test
  void testGlobFileInDirectory() {
    URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
    assertNotNull(rootDirectoryForTest);
    String testPath = rootDirectoryForTest.getPath();

    //Using globs not normal file wild cards.
    Glob searchCondition = new Glob("**.file", "**/a.file");

    //Needs the correct directory structure in /forFileFindTests
    List<File> files = underTest.getFilesRecursivelyFrom(new File(testPath), searchCondition);
    assertNotNull(files);
    assertEquals(2, files.size());
  }

  @Test
  void testGlobListFileInDirectory() {
    URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
    assertNotNull(rootDirectoryForTest);
    String testPath = rootDirectoryForTest.getPath();

    //Using globs not normal file wild cards.
    List<String> includes = Arrays.asList("**.file", "**/*.notfile");
    List<String> excludes = Arrays.asList("**/*.junk", "**/*.txt");
    Glob searchCondition = new Glob(includes, excludes);

    //Needs the correct directory structure in /forFileFindTests
    List<File> files = underTest.getFilesRecursivelyFrom(new File(testPath), searchCondition);
    assertNotNull(files);
    assertEquals(4, files.size());
  }

  @Test
  void testDeleteDirectory() throws IOException {
    URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
    assertNotNull(rootDirectoryForTest);
    String testPath = rootDirectoryForTest.getPath();

    //Using globs not normal file wild cards.
    Glob searchCondition = new Glob("**.newfile");

    File rootDir = new File(testPath);
    File newDir = new File(rootDir, "toBeRemoved");
    assertTrue(newDir.mkdir());
    File newFile = new File(newDir, "a.newfile");
    assertTrue(newFile.createNewFile());

    File extraFile = new File(newDir, "a.extrafile");
    assertTrue(extraFile.createNewFile());

    //Now check that new file can be found
    List<File> files = underTest.getFilesRecursivelyFrom(rootDir, searchCondition);
    assertNotNull(files);
    assertEquals(1, files.size());
    FileHandling fileHandling = new FileHandling(underTest);
    fileHandling.deleteMatchingFiles(newDir, "a\\.newfile");
    //Now check it has gone
    files = underTest.getFilesRecursivelyFrom(rootDir, searchCondition);
    assertNotNull(files);
    assertEquals(0, files.size());

    fileHandling.deleteContentsAndBelow(newDir, true);

  }

  @Test
  void testFileListing() {
    URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
    assertNotNull(rootDirectoryForTest);
    String testPath = rootDirectoryForTest.getPath();
    Collection<File> subdirectories = underTest.getAllSubdirectories(testPath);

    Collection<File> sourceFiles = underTest.getFilesFromDirectories(subdirectories, ".file");
    assertNotNull(sourceFiles);
    assertFalse(sourceFiles.isEmpty());

    //only three because some directories are empty and only three files have suffix ".file"
    assertEquals(3, sourceFiles.size());

    sourceFiles.forEach(dir -> reporter.log("File [" + dir.getPath() + "]"));
  }

  @Test
  void testDirectoryListing() {
    URL rootDirectoryForTest = this.getClass().getResource("/forFileFindTests");
    assertNotNull(rootDirectoryForTest);
    String testPath = rootDirectoryForTest.getPath();
    assertNotNull(testPath);

    Collection<File> subdirectories = underTest.getAllSubdirectories(testPath);
    assertEquals(6, subdirectories.size());

    subdirectories.forEach(dir -> reporter.log("Directory [" + dir.getPath() + "]"));
  }

  @Test
  void testFileNameProcessing() {
    String result = underTest.getFileNameWithoutPath(null);
    assertEquals("", result);

    result = underTest.getFileNameWithoutPath("/");
    assertEquals("", result);

    result = underTest.getFileNameWithoutPath("/root.file");
    assertEquals("root.file", result);

    result = underTest.getFileNameWithoutPath("/single/s1.file");
    assertEquals("s1.file", result);

    result = underTest.getFileNameWithoutPath("/several/items/in/a/path/s3.file");
    assertEquals("s3.file", result);
  }

  @Test
  void testDuffDirectoryListing() {
    assertThrows(IllegalArgumentException.class, () -> underTest.getAllSubdirectories(null));
  }
}
