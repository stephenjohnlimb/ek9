package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ResourceLock;

@Execution(SAME_THREAD)
@ResourceLock(value = "file_access", mode = READ_WRITE)
class FileSystemPathTest extends Common {

  private Path tempDir;
  private final List<Path> testPaths = new ArrayList<>();

  @BeforeEach
  void setUp() throws IOException {
    tempDir = Files.createTempDirectory("ek9-filesystem-test");
    testPaths.clear();
  }

  @AfterEach
  void tearDown() {
    // Clean up all test files and directories
    for (Path path : testPaths) {
      try {
        if (Files.exists(path)) {
          if (Files.isDirectory(path)) {
            try (var stream = Files.walk(path)) {
              stream.sorted(Comparator.reverseOrder()) // Delete files before directories
                  .forEach(p -> {
                    try {
                      Files.delete(p);
                    } catch (IOException _) {
                      // Ignore cleanup errors
                    }
                  });
            }
          } else {
            Files.delete(path);
          }
        }
      } catch (IOException _) {
        // Ignore cleanup errors
      }
    }

    // Clean up temp directory
    if (Files.exists(tempDir)) {
      try (var stream = Files.walk(tempDir)) {
        stream.sorted(Comparator.reverseOrder())
            .forEach(p -> {
              try {
                Files.delete(p);
              } catch (IOException _) {
                // Ignore cleanup errors
              }
            });
      } catch (IOException _) {
        // Ignore cleanup errors
      }
    }
  }

  private Path createTestFile(java.lang.String name) throws IOException {
    Path testFile = tempDir.resolve(name);
    Files.createFile(testFile);
    testPaths.add(testFile);
    return testFile;
  }

  private Path createTestDirectory(java.lang.String name) throws IOException {
    Path testDir = tempDir.resolve(name);
    Files.createDirectory(testDir);
    testPaths.add(testDir);
    return testDir;
  }

  @Test
  void testConstruction() {
    final var defaultConstructor = new FileSystemPath();
    assertUnset.accept(defaultConstructor);

    final var fromString = new FileSystemPath(String._of("test.txt"));
    assertSet.accept(fromString);
    assertEquals("test.txt", fromString._string().state);

    final var fromUnsetString = new FileSystemPath(new String());
    assertUnset.accept(fromUnsetString);
  }

  @Test
  void testCopyConstructor() {
    // Test copying from set FileSystemPath
    final var original = new FileSystemPath(String._of("test.txt"));
    assertSet.accept(original);
    
    final var copy = new FileSystemPath(original);
    assertSet.accept(copy);
    assertEquals(original._string().state, copy._string().state);
    assertTrue(original._eq(copy).state);
    
    // Test that they are independent (immutable reference sharing is OK)
    final var modified = new FileSystemPath(String._of("different.txt"));
    copy._copy(modified);
    assertFalse(original._eq(copy).state);
    
    // Test copying from unset FileSystemPath
    final var unsetOriginal = new FileSystemPath();
    assertUnset.accept(unsetOriginal);
    
    final var unsetCopy = new FileSystemPath(unsetOriginal);
    assertUnset.accept(unsetCopy);
    
    // Test copying from null (should create unset)
    final var nullCopy = new FileSystemPath((FileSystemPath) null);
    assertUnset.accept(nullCopy);
  }

  @Test
  void testFactoryMethods() {

    final var cwd = new FileSystemPath().withCurrentWorkingDirectory();
    assertSet.accept(cwd);
    assertTrue(cwd.exists().state);
    assertTrue(cwd.isAbsolute().state);

    final var tempDirectory = new FileSystemPath().withTemporaryDirectory();
    assertSet.accept(tempDirectory);
    assertTrue(tempDirectory.exists().state);
    assertTrue(tempDirectory.isAbsolute().state);
  }

  @Test
  void testFileSystemQueries() throws IOException {

    // Test with existing file
    Path testFile = createTestFile("test.txt");
    final var existingFile = new FileSystemPath(String._of(testFile.toString()));

    assertTrue(existingFile.exists().state);
    assertTrue(existingFile.isFile().state);
    assertFalse(existingFile.isDirectory().state);
    assertTrue(existingFile.isAbsolute().state);

    // Test with existing directory
    Path testDir = createTestDirectory("testdir");
    final var existingDir = new FileSystemPath(String._of(testDir.toString()));

    assertTrue(existingDir.exists().state);
    assertFalse(existingDir.isFile().state);
    assertTrue(existingDir.isDirectory().state);
    assertTrue(existingDir.isAbsolute().state);

    // Test with non-existing file
    final var nonExisting = new FileSystemPath(String._of(tempDir.resolve("nonexistent.txt").toString()));
    assertFalse(nonExisting.exists().state);
    assertFalse(nonExisting.isFile().state);
    assertFalse(nonExisting.isDirectory().state);

    // Test with unset path
    final var unsetPath = new FileSystemPath();
    assertUnset.accept(unsetPath.exists());
    assertUnset.accept(unsetPath.isFile());
    assertUnset.accept(unsetPath.isDirectory());
    assertUnset.accept(unsetPath.isAbsolute());
  }

  @Test
  void testPathOperations() throws IOException {
    final var unsetPath = new FileSystemPath();

    Path testFile = createTestFile("test.txt");
    Path testDir = createTestDirectory("testdir");

    final var filePath = new FileSystemPath(String._of(testFile.toString()));
    final var dirPath = new FileSystemPath(String._of(testDir.toString()));

    // Test startsWith
    assertTrue(filePath.startsWith(new FileSystemPath(String._of(tempDir.toString()))).state);
    assertFalse(filePath.startsWith(dirPath).state);
    assertUnset.accept(unsetPath.startsWith(dirPath));

    // Test endsWith with String
    assertTrue(filePath.endsWith(String._of("test.txt")).state);
    assertFalse(filePath.endsWith(String._of("wrong.txt")).state);
    assertUnset.accept(unsetPath.endsWith(dirPath));

    // Test endsWith with FileSystemPath
    assertTrue(filePath.endsWith(new FileSystemPath(String._of("test.txt"))).state);
    assertFalse(filePath.endsWith(new FileSystemPath(String._of("wrong.txt"))).state);

    // Test absolutePath
    final var relativePath = new FileSystemPath(String._of("relative.txt"));
    final var absolutePath = relativePath.absolutePath();
    assertSet.accept(absolutePath);
    assertTrue(absolutePath.isAbsolute().state);

    assertUnset.accept(unsetPath.absolutePath());
  }

  @Test
  void testFileCreation() {
    final var newFile = new FileSystemPath(String._of(tempDir.resolve("newfile.txt").toString()));
    testPaths.add(tempDir.resolve("newfile.txt"));

    // Create file without directory creation
    assertTrue(newFile.createFile().state);
    assertTrue(newFile.exists().state);
    assertTrue(newFile.isFile().state);

    // Try to create file that already exists
    assertFalse(newFile.createFile().state);

    // Create file with parent directory creation
    final var nestedFile =
        new FileSystemPath(String._of(tempDir.resolve("nested").resolve("deep").resolve("file.txt").toString()));
    testPaths.add(tempDir.resolve("nested"));

    assertTrue(nestedFile.createFile(Boolean._of(true)).state);
    assertTrue(nestedFile.exists().state);
    assertTrue(nestedFile.isFile().state);

    //I know I just created it this is to check unset value being passed in
    assertUnset.accept(nestedFile.createFile(new Boolean()));
  }

  @Test
  void testDirectoryCreation() {
    final var newDir = new FileSystemPath(String._of(tempDir.resolve("newdir").toString()));
    testPaths.add(tempDir.resolve("newdir"));

    // Create directory without parent creation
    assertTrue(newDir.createDirectory().state);
    assertTrue(newDir.exists().state);
    assertTrue(newDir.isDirectory().state);

    // Try to create directory that already exists
    assertFalse(newDir.createDirectory().state);

    // Create directory with parent directory creation
    final var nestedDir =
        new FileSystemPath(String._of(tempDir.resolve("nested").resolve("deep").resolve("dir").toString()));
    testPaths.add(tempDir.resolve("nested"));

    assertTrue(nestedDir.createDirectory(Boolean._of(true)).state);
    assertTrue(nestedDir.exists().state);
    assertTrue(nestedDir.isDirectory().state);
  }

  @Test
  void testEqualityOperators() throws IOException {
    Path testFile = createTestFile("test.txt");

    final var path1 = new FileSystemPath(String._of(testFile.toString()));
    final var path2 = new FileSystemPath(String._of(testFile.toString()));
    final var path3 = new FileSystemPath(String._of(tempDir.resolve("other.txt").toString()));

    // Test equality
    assertTrue(path1._eq(path2).state);
    assertFalse(path1._eq(path3).state);

    // Test inequality
    assertFalse(path1._neq(path2).state);
    assertTrue(path1._neq(path3).state);

    // Test with unset paths
    final var unset1 = new FileSystemPath();
    final var unset2 = new FileSystemPath();
    assertUnset.accept(unset1._eq(unset2));
    assertUnset.accept(unset1._neq(path1));
  }

  @Test
  void testComparisonOperators() {
    final var pathA = new FileSystemPath(String._of("a.txt"));
    final var pathB = new FileSystemPath(String._of("b.txt"));
    final var pathC = new FileSystemPath(String._of("a.txt"));

    // Test comparison
    assertTrue(pathA._cmp(pathB).state < 0);
    assertTrue(pathB._cmp(pathA).state > 0);
    assertEquals(0, pathA._cmp(pathC).state);

    //Test via any compare.
    Any any = pathA;
    assertEquals(0, pathA._cmp(any).state);

    //Make little sense - hence unset.
    assertUnset.accept(pathA._cmp(new String()));
    assertUnset.accept(pathA._cmp(new FileSystemPath()));

    // Test less than
    assertTrue(pathA._lt(pathB).state);
    assertFalse(pathB._lt(pathA).state);
    assertUnset.accept(pathA._lt(new FileSystemPath()));

    // Test less than or equal
    assertTrue(pathA._lteq(pathB).state);
    assertTrue(pathA._lteq(pathC).state);
    assertFalse(pathB._lteq(pathA).state);
    assertUnset.accept(pathA._lteq(new FileSystemPath()));

    // Test greater than
    assertTrue(pathB._gt(pathA).state);
    assertFalse(pathA._gt(pathB).state);
    assertUnset.accept(pathA._gt(new FileSystemPath()));

    // Test greater than or equal
    assertTrue(pathB._gteq(pathA).state);
    assertTrue(pathA._gteq(pathC).state);
    assertFalse(pathA._gteq(pathB).state);
    assertUnset.accept(pathA._gteq(new FileSystemPath()));
  }

  @Test
  void testPathAddition() {
    final var basePath = new FileSystemPath(String._of(tempDir.toString()));
    final var subPath = new FileSystemPath(String._of("subdir"));
    final var fileName = String._of("file.txt");

    // Test addition with FileSystemPath
    final var combined1 = basePath._add(subPath);
    assertSet.accept(combined1);
    assertTrue(combined1._string().state.endsWith("subdir"));

    //Check adding something unset results in unset.
    assertUnset.accept(basePath._add(new FileSystemPath()));
    assertUnset.accept(basePath._add(new String()));

    // Test addition with String
    final var combined2 = basePath._add(fileName);
    assertSet.accept(combined2);
    assertTrue(combined2._string().state.endsWith("file.txt"));

    // Test addition assignment with FileSystemPath
    final var mutablePath1 = new FileSystemPath(String._of(tempDir.toString()));
    mutablePath1._addAss(subPath);
    assertTrue(mutablePath1._string().state.endsWith("subdir"));

    //Now corrupt it with unset
    mutablePath1._addAss(new FileSystemPath());
    assertUnset.accept(mutablePath1);

    // Test addition assignment with String
    final var mutablePath2 = new FileSystemPath(String._of(tempDir.toString()));
    mutablePath2._addAss(fileName);
    assertTrue(mutablePath2._string().state.endsWith("file.txt"));

    //Again corrupt it
    mutablePath2._addAss(new String());
    assertUnset.accept(mutablePath2);

  }

  @Test
  void testStringOperator() throws IOException {
    Path testFile = createTestFile("test.txt");

    final var path = new FileSystemPath(String._of(testFile.toString()));
    final var stringRep = path._string();
    assertSet.accept(stringRep);
    assertEquals(testFile.toString(), stringRep.state);

    // Test with unset path
    final var unsetPath = new FileSystemPath();
    assertUnset.accept(unsetPath._string());
  }

  @Test
  void testHashCodeOperator() throws IOException {
    Path testFile = createTestFile("test.txt");

    final var path = new FileSystemPath(String._of(testFile.toString()));
    final var hashCode = path._hashcode();
    assertSet.accept(hashCode);
    assertEquals(testFile.hashCode(), hashCode.state);

    // Test with unset path
    final var unsetPath = new FileSystemPath();
    assertUnset.accept(unsetPath._hashcode());
  }

  @Test
  void testIsSetOperator() throws IOException {
    Path testFile = createTestFile("test.txt");

    final var setPath = new FileSystemPath(String._of(testFile.toString()));
    assertTrue(setPath._isSet().state);

    final var unsetPath = new FileSystemPath();
    assertFalse(unsetPath._isSet().state);
  }

  @Test
  void testFuzzyOperator() {
    final var path1 = new FileSystemPath(String._of("similar.txt"));
    final var path2 = new FileSystemPath(String._of("similar.txt"));
    final var path3 = new FileSystemPath(String._of("different.txt"));

    // Test fuzzy comparison
    final var fuzzy1 = path1._fuzzy(path2);
    assertSet.accept(fuzzy1);
    assertEquals(0, fuzzy1.state); // Identical strings

    final var fuzzy2 = path1._fuzzy(path3);
    assertSet.accept(fuzzy2);
    assertTrue(fuzzy2.state > 0); // Different strings

    // Test with unset path
    final var unsetPath = new FileSystemPath();
    assertUnset.accept(unsetPath._fuzzy(path1));
  }

  @Test
  void testErrorConditions() {
    // Test with invalid string input
    final var invalidPath = new FileSystemPath((org.ek9.lang.String) null);
    assertNotNull(invalidPath);
    assertUnset.accept(invalidPath);

    // Test operations on unset paths
    final var unsetPath = new FileSystemPath();
    assertUnset.accept(unsetPath.createFile());
    assertUnset.accept(unsetPath.createDirectory());
    assertUnset.accept(unsetPath.exists());
    assertUnset.accept(unsetPath.isFile());
    assertUnset.accept(unsetPath.isDirectory());
  }

  @Test
  void testRelativePathOperations() {
    final var relativePath = new FileSystemPath(String._of("relative" + File.separator + "path.txt"));
    assertSet.accept(relativePath);
    assertFalse(relativePath.isAbsolute().state);

    final var absolutePath = relativePath.absolutePath();
    assertSet.accept(absolutePath);
    assertTrue(absolutePath.isAbsolute().state);
  }

  @Test
  void testPathSeparatorHandling() {
    // Test with different path separators
    final var unixPath = new FileSystemPath(String._of("path/to/file.txt"));
    assertNotNull(unixPath);
    final var windowsPath = new FileSystemPath(String._of("path\\to\\file.txt"));
    assertNotNull(windowsPath);

    assertSet.accept(unixPath);
    assertSet.accept(windowsPath);

    // Test path addition with mixed separators
    final var basePath = new FileSystemPath(String._of("base"));
    final var addedPath = basePath._add(String._of("sub/dir"));
    assertSet.accept(addedPath);
  }

  @Test
  void testCopyOperators() {
    // Test :=: (copy) operator with valid arguments
    final var target = new FileSystemPath(String._of("original.txt"));
    final var source = new FileSystemPath(String._of("source.txt"));
    
    assertSet.accept(target);
    assertSet.accept(source);
    assertFalse(target._eq(source).state);
    
    target._copy(source);
    assertTrue(target._eq(source).state);
    assertEquals(source._string().state, target._string().state);
    
    // Test :=: (copy) operator with unset source
    final var unsetSource = new FileSystemPath();
    target._copy(unsetSource);
    assertUnset.accept(target);
    
    // Test :=: (copy) operator with null source
    final var anotherTarget = new FileSystemPath(String._of("another.txt"));
    anotherTarget._copy(null);
    assertUnset.accept(anotherTarget);
    
    // Test :^: (replace) operator - should behave same as copy
    final var replaceTarget = new FileSystemPath(String._of("replace.txt"));
    final var replaceSource = new FileSystemPath(String._of("new.txt"));
    
    replaceTarget._replace(replaceSource);
    assertTrue(replaceTarget._eq(replaceSource).state);
    
    // Test replace with unset source
    replaceTarget._replace(new FileSystemPath());
    assertUnset.accept(replaceTarget);
  }

  @Test
  void testMergeOperators() {
    // Test :~: (merge) operator with unset target
    final var unsetTarget = new FileSystemPath();
    final var mergeSource = new FileSystemPath(String._of("merge.txt"));
    
    unsetTarget._merge(mergeSource);
    assertTrue(unsetTarget._eq(mergeSource).state);
    
    // Test :~: (merge) operator with set target (should add)
    final var setTarget = new FileSystemPath(String._of(tempDir.toString()));
    final var pathToAdd = new FileSystemPath(String._of("subdir"));
    
    setTarget._merge(pathToAdd);
    assertTrue(setTarget._string().state.endsWith("subdir"));
    
    // Test | (pipe) operator - should behave same as merge
    final var pipeTarget = new FileSystemPath();
    final var pipeSource = new FileSystemPath(String._of("pipe.txt"));
    
    pipeTarget._pipe(pipeSource);
    assertTrue(pipeTarget._eq(pipeSource).state);
    
    // Test pipe with set target
    final var setPipeTarget = new FileSystemPath(String._of(tempDir.toString()));
    final var pipeAddition = new FileSystemPath(String._of("added"));
    
    setPipeTarget._pipe(pipeAddition);
    assertTrue(setPipeTarget._string().state.endsWith("added"));
    
    // Test merge with invalid argument
    final var mergeTestTarget = new FileSystemPath(String._of("test.txt"));
    final var originalValue = mergeTestTarget._string().state;
    
    mergeTestTarget._merge(null);
    assertEquals(originalValue, mergeTestTarget._string().state); // Should remain unchanged
    
    mergeTestTarget._merge(new FileSystemPath());
    assertEquals(originalValue, mergeTestTarget._string().state); // Should remain unchanged
  }

  @Test
  void testFilePermissions() throws IOException {
    // Test with existing file
    Path testFile = createTestFile("permission_test.txt");
    final var existingFile = new FileSystemPath(String._of(testFile.toString()));
    
    // Test readable - should be true for created file
    assertTrue(existingFile.isReadable().state);
    
    // Test writable - should be true for created file
    assertTrue(existingFile.isWritable().state);
    
    // Test executable - depends on OS, but should return a boolean
    assertSet.accept(existingFile.isExecutable());
    
    // Test with existing directory
    Path testDir = createTestDirectory("permission_test_dir");
    final var existingDir = new FileSystemPath(String._of(testDir.toString()));
    
    // Test readable on directory
    assertTrue(existingDir.isReadable().state);
    
    // Test writable on directory
    assertTrue(existingDir.isWritable().state);
    
    // Test executable on directory (usually true for directories)
    assertTrue(existingDir.isExecutable().state);
    
    // Test with non-existing file
    final var nonExisting = new FileSystemPath(String._of(tempDir.resolve("nonexistent.txt").toString()));
    assertUnset.accept(nonExisting.isReadable());
    assertUnset.accept(nonExisting.isWritable());
    assertUnset.accept(nonExisting.isExecutable());
    
    // Test with unset path
    final var unsetPath = new FileSystemPath();
    assertUnset.accept(unsetPath.isReadable());
    assertUnset.accept(unsetPath.isWritable());
    assertUnset.accept(unsetPath.isExecutable());
    
    // Test with relative path that doesn't exist as absolute
    final var relativePath = new FileSystemPath(String._of("relative.txt"));
    assertUnset.accept(relativePath.isReadable());
    assertUnset.accept(relativePath.isWritable());
    assertUnset.accept(relativePath.isExecutable());
  }

  @Test
  void testIntegrationOperatorChaining() throws IOException {
    // Test chaining copy and merge operations
    final var base = new FileSystemPath(String._of(tempDir.toString()));
    final var sub1 = new FileSystemPath(String._of("sub1"));
    final var sub2 = new FileSystemPath(String._of("sub2"));
    final var filename = new FileSystemPath(String._of("test.txt"));
    
    // Chain operations: copy base, merge sub1, merge sub2, merge filename
    final var result = new FileSystemPath();
    result._copy(base);
    result._merge(sub1);
    result._merge(sub2);
    result._merge(filename);
    
    assertTrue(result._string().state.contains("sub1"));
    assertTrue(result._string().state.contains("sub2"));
    assertTrue(result._string().state.endsWith("test.txt"));
    
    // Test replace after building path
    final var newPath = new FileSystemPath(String._of("replacement.txt"));
    result._replace(newPath);
    assertTrue(result._eq(newPath).state);
    
    // Test pipe operations
    final var pipeResult = new FileSystemPath();
    pipeResult._pipe(base);
    pipeResult._pipe(sub1);
    pipeResult._pipe(filename);
    
    assertTrue(pipeResult._string().state.contains("sub1"));
    assertTrue(pipeResult._string().state.endsWith("test.txt"));
    
    // Test mixed operations with existing file
    Path testFile = createTestFile("chain_test.txt");
    final var existingPath = new FileSystemPath(String._of(testFile.toString()));
    
    // Copy existing path and verify it works
    final var copied = new FileSystemPath();
    copied._copy(existingPath);
    assertTrue(copied.exists().state);
    assertTrue(copied.isFile().state);
    assertTrue(copied.isReadable().state);
    assertTrue(copied.isWritable().state);
  }

  @Test
  void testAsJson() {
    // Test unset FileSystemPath - should return unset JSON
    final var unsetPath = new FileSystemPath();
    assertNotNull(unsetPath);
    final var unsetJson = unsetPath._json();
    assertUnset.accept(unsetJson);

    // Test set FileSystemPath with valid path string
    final var validPath = new FileSystemPath(String._of("/tmp/test/json_test.txt"));
    final var validJson = validPath._json();
    assertSet.accept(validJson);
    
    // Verify JSON content matches string representation
    final var expectedJsonContent = validPath._string()._json();
    assertTrue.accept(validJson._eq(expectedJsonContent));
    
    // Test JSON structure - should be a string value
    assertTrue.accept(validJson.valueNature());
    assertFalse.accept(validJson.objectNature());
    assertFalse.accept(validJson.arrayNature());
    
    // Test with different path types
    final var dirPath = new FileSystemPath(String._of("/home/user/documents"));
    final var dirJson = dirPath._json();
    assertSet.accept(dirJson);
    assertTrue.accept(dirJson.valueNature());
    
    // Test with relative path
    final var relativePath = new FileSystemPath(String._of("./relative/path.txt"));
    final var relativeJson = relativePath._json();
    assertSet.accept(relativeJson);
    assertTrue.accept(relativeJson.valueNature());
  }
}