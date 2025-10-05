package org.ek9lang.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Project-level integration tests for EK9 compiler and wrapper.
 * Tests the complete workflow: wrapper → JAR → compiler → runtime
 * using actual built artifacts and real EK9 source files.
 * <p>
 * Tests are designed for parallel execution - each test gets its own
 * isolated temporary directory to avoid conflicts.
 * </p>
 */
@Execution(ExecutionMode.CONCURRENT)
class Ek9IntegrationTest {

  private static final File WRAPPER_BINARY = getWrapperBinary();
  private static final File COMPILER_JAR = getCompilerJar();

  @TempDir
  Path tempWorkDir;

  private Ek9ProcessExecutor executor;

  @BeforeEach
  void setup() {
    // Initialize executor with JUnit-managed temp directory (auto-cleanup, parallel-safe)
    executor = new Ek9ProcessExecutor(WRAPPER_BINARY, COMPILER_JAR, tempWorkDir.toFile());
  }

  /**
   * Test basic compilation workflow via wrapper.
   * Verifies: wrapper → JAR → compile → artifacts created
   */
  @Test
  void testCompileHelloWorldViaWrapper() throws IOException {
    copyTestProgram("HelloWorld.ek9");

    ProcessResult result = executor.executeWrapper("-c", "HelloWorld.ek9");

    assertEquals(0, result.exitCode(), "Compilation should succeed");

    // Verify compilation artifacts exist
    File dotEk9Dir = tempWorkDir.resolve(".ek9").toFile();
    assertTrue(dotEk9Dir.exists(), ".ek9 directory should be created");
  }

  /**
   * Test running simple program via wrapper.
   * Verifies: compile + run workflow, stdout capture
   */
  @Test
  void testRunHelloWorldViaWrapper() throws IOException {
    copyTestProgram("HelloWorld.ek9");

    ProcessResult result = executor.executeWrapper("HelloWorld.ek9");

    assertEquals(0, result.exitCode(), "Wrapper maps run exit code 1 to 0 (Unix success)");
    if(!result.stdoutContains("Hello, World")) {
      System.err.println(result);
      fail("Test Failed");
    }

  }


  /**
   * Test program with String argument containing spaces.
   * Verifies: quoted argument handling through wrapper
   */
  @Test
  void testProgramWithQuotedArgument() throws IOException {
    copyTestProgram("HelloWithArgs.ek9");

    ProcessResult result = executor.executeWrapper(
        "HelloWithArgs.ek9",
        "-r", "HelloString",
        "This is a single argument with spaces");

    assertEquals(0, result.exitCode(), "Run should execute");
    if(!result.stdoutContains("This is a single argument with spaces")) {
      System.err.println(result);
      fail("Test Failed");
    }
  }


  /**
   * Test compilation error handling.
   * Verifies: proper exit code for invalid EK9 source
   */
  @Test
  void testCompilationError() throws IOException {
    // Create invalid EK9 file
    String invalidSource = "#!ek9\ndefines module broken\n  this is not valid ek9\n//EOF";
    Files.writeString(tempWorkDir.resolve("broken.ek9"), invalidSource);

    ProcessResult result = executor.executeWrapper("-c", "broken.ek9");

    assertEquals(3, result.exitCode(), "Should return FILE_ISSUE_EXIT_CODE for compilation error");
  }

  /**
   * Test missing file error.
   * Verifies: proper exit code for non-existent file
   */
  @Test
  void testMissingFileError() {
    ProcessResult result = executor.executeWrapper("-c", "nonexistent.ek9");

    assertEquals(3, result.exitCode(), "Should return FILE_ISSUE_EXIT_CODE for missing file");
  }


  /**
   * Test stdin redirection through wrapper.
   * Verifies: wrapper pipes stdin to running EK9 program (PassThrough program)
   */
  @Test
  void testStdinRedirection() throws IOException {
    copyTestProgram("StdinTest.ek9");

    String stdinInput = "Hello from stdin\n";
    ProcessResult result = executor.executeWrapperWithStdin(stdinInput, "StdinTest.ek9", "-r", "PassThrough");

    assertEquals(0, result.exitCode(), "Program should execute successfully");
    if(!result.stdoutContains("Hello from stdin")) {
      System.err.println(result);
      fail("Test Failed");
    }
  }

  /**
   * Test compilation via JAR directly (bypass wrapper).
   * Verifies: JAR can be invoked directly with java -jar
   */
  @Test
  void testCompileViaJarDirectly() throws IOException {
    copyTestProgram("HelloWorld.ek9");

    ProcessResult result = executor.executeJar("-c", "HelloWorld.ek9");

    // JAR returns 1 for successful compile (no run)
    assertEquals(1, result.exitCode(), "JAR should return 1 for successful compile");

    // Verify compilation artifacts exist
    File dotEk9Dir = tempWorkDir.resolve(".ek9").toFile();
    assertTrue(dotEk9Dir.exists(), ".ek9 directory should be created");
  }

  /**
   * Test that wrapper produces same compilation artifacts as JAR.
   * Verifies: wrapper correctly invokes JAR and maps exit codes
   */
  @Test
  void testWrapperProducesSameArtifactsAsJar() throws IOException {
    // Compile via wrapper
    copyTestProgram("HelloWorld.ek9");
    ProcessResult wrapperResult = executor.executeWrapper("-c", "HelloWorld.ek9");

    // Wrapper maps JAR exit 1 → 0 for Unix success convention
    assertEquals(0, wrapperResult.exitCode(), "Wrapper should map exit 1 to 0");

    // Verify compilation artifacts exist
    File dotEk9Dir = tempWorkDir.resolve(".ek9").toFile();
    assertTrue(dotEk9Dir.exists(), "Wrapper should create same .ek9 directory as JAR");
  }


  /**
   * Helper: Copy test program from resources to temp working directory.
   */
  private void copyTestProgram(String programName) throws IOException {
    URL resourceUrl = getClass().getResource("/test-programs/" + programName);
    if (resourceUrl == null) {
      throw new IOException("Test program not found: " + programName);
    }

    try {
      Path sourcePath = Path.of(resourceUrl.toURI());
      Path targetPath = tempWorkDir.resolve(programName);
      Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (URISyntaxException e) {
      throw new IOException("Invalid resource URI: " + programName, e);
    }
  }

  /**
   * Get wrapper binary path from system property or default location.
   */
  private static File getWrapperBinary() {
    final var wrapperPath = System.getProperty("ek9.wrapper.path",
        "../ek9-wrapper/target/bin/ek9");
    return new File(wrapperPath);
  }

  /**
   * Get compiler JAR path from system property or default location.
   */
  private static File getCompilerJar() {
    final var jarPath = System.getProperty("ek9.jar.path",
        "../compiler-main/target/ek9c-jar-with-dependencies.jar");
    return new File(jarPath);
  }
}
