package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

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
class TextFileTest extends Common {

  private Path tempDir;
  private final List<Path> testFiles = new ArrayList<>();

  @BeforeEach
  void setUp() throws IOException {
    tempDir = Files.createTempDirectory("ek9-textfile-test");
    testFiles.clear();
  }

  @AfterEach
  void tearDown() {
    // Clean up all test files and directories
    for (Path path : testFiles) {
      try {
        if (Files.exists(path)) {
          if (Files.isDirectory(path)) {
            try (var stream = Files.walk(path)) {
              stream.sorted(Comparator.reverseOrder())
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
    try {
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
        }
      }
    } catch (IOException _) {
      // Ignore cleanup errors
    }
  }

  private Path createTestFile(java.lang.String name, java.lang.String content) throws IOException {
    Path testFile = tempDir.resolve(name);
    Files.write(testFile, content.getBytes());
    testFiles.add(testFile);
    return testFile;
  }

  private Path createTestDirectory(java.lang.String name) throws IOException {
    Path testDir = tempDir.resolve(name);
    Files.createDirectory(testDir);
    testFiles.add(testDir);
    return testDir;
  }

  // Constructor Tests

  @Test
  void testDefaultConstructor() {
    TextFile textFile = new TextFile();
    assertNotNull(textFile);
    assertUnset.accept(textFile);
  }

  @Test
  void testFactoryMethodWithValidFile() throws IOException {
    Path testFile = createTestFile("test.txt", "Hello World");

    TextFile textFile = TextFile._of(testFile);
    assertNotNull(textFile);
    assertSet.accept(textFile);
  }

  @Test
  void testFactoryMethodWithNonExistentFile() {
    java.lang.String nonExistentPath = tempDir.resolve("nonexistent.txt").toString();

    TextFile textFile = TextFile._of(nonExistentPath);
    assertNotNull(textFile);
    assertUnset.accept(textFile);
  }

  @Test
  void testFactoryMethodWithDirectory() throws IOException {
    Path testDir = createTestDirectory("testdir");

    TextFile textFile = TextFile._of(testDir);
    assertNotNull(textFile);
    // NOTE: Current implementation sets TextFile for directories
    // This may be inconsistent with String constructor behavior
    assertSet.accept(textFile);
  }

  @Test
  void testStringConstructorWithUnsetString() {
    TextFile textFile = new TextFile(new String());
    assertNotNull(textFile);
    assertUnset.accept(textFile);
  }

  @Test
  void testFileSystemPathConstructorWithValidPath() throws IOException {
    Path testFile = createTestFile("test.txt", "Hello World");
    FileSystemPath path = new FileSystemPath(String._of(testFile.toString()));

    TextFile textFile = new TextFile(path);
    assertNotNull(textFile);
    assertSet.accept(textFile);
  }

  @Test
  void testFileSystemPathConstructorWithUnsetPath() {
    FileSystemPath path = new FileSystemPath();

    TextFile textFile = new TextFile(path);
    assertNotNull(textFile);
    assertUnset.accept(textFile);
  }

  @Test
  void testCopyConstructorWithValidFile() throws IOException {
    Path testFile = createTestFile("copy_original.txt", "copy test content");
    TextFile original = TextFile._of(testFile);

    TextFile copy = new TextFile(original);

    // Verify both are set and have same content
    assertNotNull(copy);
    assertSet.accept(original);
    assertSet.accept(copy);
    assertEquals(original._string().state, copy._string().state);
    assertEquals(original._len().state, copy._len().state);
  }

  @Test
  void testCopyConstructorWithUnsetFile() {
    TextFile original = new TextFile();
    TextFile copy = new TextFile(original);

    // Both should be unset
    assertNotNull(copy);
    assertUnset.accept(original);
    assertUnset.accept(copy);
  }

  @Test
  void testCopyConstructorIndependence() throws IOException {
    Path testFile = createTestFile("independence.txt", "independence test");
    TextFile original = TextFile._of(testFile);
    TextFile copy = new TextFile(original);

    // Both start as set
    assertNotNull(copy);
    assertSet.accept(original);
    assertSet.accept(copy);

    // They should be independent objects
    assertNotSame(original, copy);

    // But reference the same file content
    assertEquals(original._string().state, copy._string().state);
    assertEquals(original._len().state, copy._len().state);

    // Both should be able to read the same content
    StringInput originalInput = original.input();
    StringInput copyInput = copy.input();

    assertTrue(originalInput._isSet().state);
    assertTrue(copyInput._isSet().state);

    assertEquals(originalInput.next().state, copyInput.next().state);

    originalInput._close();
    copyInput._close();
  }

  // File Trait Implementation Tests

  @Test
  void testIsWritableWithWritableFile() throws IOException {
    Path testFile = createTestFile("writable.txt", "content");
    TextFile textFile = TextFile._of(testFile);

    Boolean result = textFile.isWritable();
    assertSet.accept(result);
    assertTrue(result.state);
  }

  @Test
  void testIsWritableWithUnsetFile() {
    TextFile textFile = new TextFile();
    assertNotNull(textFile);

    Boolean result = textFile.isWritable();
    assertUnset.accept(result);
  }

  @Test
  void testIsReadableWithReadableFile() throws IOException {
    Path testFile = createTestFile("readable.txt", "content");
    TextFile textFile = TextFile._of(testFile);

    Boolean result = textFile.isReadable();
    assertSet.accept(result);
    assertTrue(result.state);
  }

  @Test
  void testIsReadableWithUnsetFile() {
    TextFile textFile = new TextFile();
    assertNotNull(textFile);

    Boolean result = textFile.isReadable();
    assertUnset.accept(result);
  }

  @Test
  void testIsExecutableWithFile() throws IOException {
    Path testFile = createTestFile("executable.txt", "content");
    TextFile textFile = TextFile._of(testFile);
    assertNotNull(textFile);

    Boolean result = textFile.isExecutable();
    assertSet.accept(result);
    // Note: Text files are typically not executable
  }

  @Test
  void testIsExecutableWithUnsetFile() {
    TextFile textFile = new TextFile();
    assertNotNull(textFile);

    Boolean result = textFile.isExecutable();
    assertUnset.accept(result);
  }

  @Test
  void testLastModifiedWithValidFile() throws IOException {
    Path testFile = createTestFile("timestamped.txt", "content");
    TextFile textFile = TextFile._of(testFile);

    DateTime result = textFile.lastModified();
    assertSet.accept(result);
    assertNotNull(result.state);
  }

  @Test
  void testLastModifiedWithUnsetFile() {
    TextFile textFile = new TextFile();
    assertNotNull(textFile);

    DateTime result = textFile.lastModified();
    assertUnset.accept(result);
  }

  @Test
  void testLengthOperatorWithValidFile() throws IOException {
    java.lang.String content = "Hello World";
    Path testFile = createTestFile("sized.txt", content);
    TextFile textFile = TextFile._of(testFile);

    Integer result = textFile._len();
    assertSet.accept(result);
    assertEquals(content.length(), (int) result.state);
  }

  @Test
  void testLengthOperatorWithUnsetFile() {
    TextFile textFile = new TextFile();
    assertNotNull(textFile);

    Integer result = textFile._len();
    assertUnset.accept(result);
  }

  @Test
  void testStringOperatorWithValidFile() throws IOException {
    Path testFile = createTestFile("pathtest.txt", "content");
    TextFile textFile = TextFile._of(testFile);

    String result = textFile._string();
    assertSet.accept(result);
    assertEquals(testFile.toAbsolutePath().toString(), result.state);
  }

  @Test
  void testStringOperatorWithUnsetFile() {
    TextFile textFile = new TextFile();
    assertNotNull(textFile);

    String result = textFile._string();
    assertUnset.accept(result);
  }

  @Test
  void testHashcodeOperatorWithValidFile() throws IOException {
    Path testFile = createTestFile("hashtest.txt", "content");
    TextFile textFile = TextFile._of(testFile);
    assertNotNull(textFile);

    Integer result = textFile._hashcode();
    assertSet.accept(result);
  }

  @Test
  void testHashcodeOperatorWithUnsetFile() {
    TextFile textFile = new TextFile();
    assertNotNull(textFile);

    Integer result = textFile._hashcode();
    assertUnset.accept(result);
  }

  // TextFile Specific Tests

  @Test
  void testInputWithReadableFile() throws IOException {
    java.lang.String expectedContent = "Hello World\nLine 2\nLine 3\n";
    Path testFile = createTestFile("input.txt", expectedContent);
    TextFile textFile = TextFile._of(testFile);

    StringInput input = textFile.input();
    assertTrue(input._isSet().state);

    // Read and verify content line by line
    assertTrue(input.hasNext().state);
    assertEquals("Hello World", input.next().state);

    assertTrue(input.hasNext().state);
    assertEquals("Line 2", input.next().state);

    assertTrue(input.hasNext().state);
    assertEquals("Line 3", input.next().state);

    // Should have no more lines
    assertFalse(input.hasNext().state);

    input._close();
  }

  @Test
  void testInputWithUnsetFile() {
    TextFile textFile = new TextFile();

    StringInput result = textFile.input();
    assertFalse(result._isSet().state);
  }

  @Test
  void testOutputWithWritableFile() throws IOException {
    Path testFile = createTestFile("output.txt", "initial content");
    TextFile textFile = TextFile._of(testFile);

    StringOutput output = textFile.output();
    assertTrue(output._isSet().state);

    // Write content using different methods
    output.println(String._of("Line 1"));
    output.print(String._of("Line 2"));
    output.println(String._of(""));  // Complete Line 2 with newline
    output.println(String._of("Line 3"));
    output._close();

    // Verify content was written correctly
    java.lang.String expectedContent = "Line 1\nLine 2\nLine 3\n";
    java.lang.String actualContent = Files.readString(testFile);
    assertEquals(expectedContent, actualContent);
  }

  @Test
  void testOutputWithUnsetFile() {
    TextFile textFile = new TextFile();

    StringOutput result = textFile.output();
    assertFalse(result._isSet().state);
  }

  @Test
  void testWriteAndReadRoundTrip() throws IOException {
    Path testFile = createTestFile("roundtrip.txt", "");
    TextFile textFile = TextFile._of(testFile);

    // Write content using StringOutput
    StringOutput output = textFile.output();
    assertTrue(output._isSet().state);

    output.println(String._of("Round trip test"));
    output.println(String._of("Second line"));
    output.println(String._of("Third line with data"));
    output._close();

    // Read content back using StringInput
    StringInput input = textFile.input();
    assertTrue(input._isSet().state);

    assertTrue(input.hasNext().state);
    assertEquals("Round trip test", input.next().state);

    assertTrue(input.hasNext().state);
    assertEquals("Second line", input.next().state);

    assertTrue(input.hasNext().state);
    assertEquals("Third line with data", input.next().state);

    // Should have no more lines
    assertFalse(input.hasNext().state);

    input._close();

    // Also verify by reading file directly
    java.lang.String fileContent = Files.readString(testFile);
    assertEquals("Round trip test\nSecond line\nThird line with data\n", fileContent);
  }

  // State Management Tests

  @Test
  void testStateTransitionFromUnsetToSet() throws IOException {
    TextFile textFile = new TextFile();
    assertNotNull(textFile);
    assertUnset.accept(textFile);

    // Create file and create new TextFile instance
    Path testFile = createTestFile("transition.txt", "content");
    TextFile setTextFile = TextFile._of(testFile);
    assertSet.accept(setTextFile);
  }

  @Test
  void testConsistentBehaviorAcrossOperations() throws IOException {
    Path testFile = createTestFile("consistent.txt", "test content");
    TextFile textFile = TextFile._of(testFile);

    // All operations should be consistent with set state
    assertSet.accept(textFile);
    assertSet.accept(textFile.isReadable());
    assertSet.accept(textFile.isWritable());
    assertSet.accept(textFile.lastModified());
    assertSet.accept(textFile._len());
    assertSet.accept(textFile._string());
    assertSet.accept(textFile._hashcode());
    assertTrue(textFile.input()._isSet().state);
    assertTrue(textFile.output()._isSet().state);
  }

  @Test
  void testUnsetBehaviorAcrossOperations() {
    TextFile textFile = new TextFile();

    // All operations should be consistent with unset state
    assertUnset.accept(textFile);
    assertUnset.accept(textFile.isReadable());
    assertUnset.accept(textFile.isWritable());
    assertUnset.accept(textFile.isExecutable());
    assertUnset.accept(textFile.lastModified());
    assertUnset.accept(textFile._len());
    assertUnset.accept(textFile._string());
    assertUnset.accept(textFile._hashcode());
    assertFalse(textFile.input()._isSet().state);
    assertFalse(textFile.output()._isSet().state);
  }

  // Line Ending and Content Tests

  @Test
  void testMultipleLineWriting() throws IOException {
    Path testFile = createTestFile("multiline.txt", "");
    TextFile textFile = TextFile._of(testFile);

    StringOutput output = textFile.output();
    assertTrue(output._isSet().state);

    // Write multiple lines with different patterns
    output.println(String._of("First line"));
    output.println(String._of(""));  // Empty line
    output.println(String._of("Third line"));
    output.print(String._of("Fourth line without newline"));
    output.println(String._of(""));  // Add newline to fourth line
    output._close();

    // Verify content
    java.lang.String expectedContent = "First line\n\nThird line\nFourth line without newline\n";
    java.lang.String actualContent = Files.readString(testFile);
    assertEquals(expectedContent, actualContent);

    // Also verify by reading back with StringInput
    StringInput input = textFile.input();
    assertTrue(input._isSet().state);

    assertEquals("First line", input.next().state);
    assertEquals("", input.next().state);  // Empty line
    assertEquals("Third line", input.next().state);
    assertEquals("Fourth line without newline", input.next().state);
    assertFalse(input.hasNext().state);

    input._close();
  }

  @Test
  void testEmptyLineHandling() throws IOException {
    Path testFile = createTestFile("emptylines.txt", "");
    TextFile textFile = TextFile._of(testFile);

    StringOutput output = textFile.output();
    output.println(String._of(""));  // Empty line
    output.println(String._of(""));  // Another empty line
    output.println(String._of("Not empty"));
    output.println(String._of(""));  // Final empty line
    output._close();

    // Verify by reading back
    StringInput input = textFile.input();
    assertEquals("", input.next().state);
    assertEquals("", input.next().state);
    assertEquals("Not empty", input.next().state);
    assertEquals("", input.next().state);
    assertFalse(input.hasNext().state);

    input._close();
  }

  @Test
  void testLineEndingPreservation() throws IOException {
    java.lang.String contentWithNewlines = "Line 1\nLine 2\nLine 3\n";
    Path testFile = createTestFile("lineendings.txt", contentWithNewlines);
    TextFile textFile = TextFile._of(testFile);

    // Read content and verify line endings are handled correctly
    StringInput input = textFile.input();
    assertTrue(input._isSet().state);

    assertEquals("Line 1", input.next().state);
    assertEquals("Line 2", input.next().state);
    assertEquals("Line 3", input.next().state);
    assertFalse(input.hasNext().state);

    input._close();

    // Verify file content is preserved exactly
    java.lang.String actualContent = Files.readString(testFile);
    assertEquals(contentWithNewlines, actualContent);
  }

  // Integration and Edge Case Tests

  @Test
  void testFileSystemPathIntegration() throws IOException {
    Path testFile = createTestFile("integration.txt", "content");
    FileSystemPath path = new FileSystemPath(String._of(testFile.toString()));
    TextFile textFile = new TextFile(path);

    // Verify both path and textfile work correctly
    assertTrue(path.exists().state);
    assertTrue(path.isFile().state);
    assertSet.accept(textFile);
    assertTrue(textFile.isReadable().state);
  }

  @Test
  void testEmptyFileHandling() throws IOException {
    Path testFile = createTestFile("empty.txt", "");
    TextFile textFile = TextFile._of(testFile);

    assertSet.accept(textFile);
    Integer length = textFile._len();
    assertSet.accept(length);
    assertEquals(0, (int) length.state);
  }

  @Test
  void testLargeFileHandling() throws IOException {
    java.lang.String content = "A".repeat(10000);
    Path testFile = createTestFile("large.txt", content);
    TextFile textFile = TextFile._of(testFile);

    assertSet.accept(textFile);
    Integer length = textFile._len();
    assertSet.accept(length);
    assertEquals(content.length(), (int) length.state);
  }
}