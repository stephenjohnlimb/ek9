package org.ek9lang.compiler.bytecode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.ek9lang.core.TargetArchitecture;
import org.junit.jupiter.api.Test;

/**
 * Abstract base class for bytecode tests that also execute the compiled program.
 * Extends AbstractBytecodeGenerationTest to add in-JVM execution capability.
 *
 * <h2>File-Based Test Configuration</h2>
 * <p>Test cases are discovered from files in the test directory:</p>
 * <ul>
 *   <li>{@code commandline_arg_<case>.txt} - Contains arguments (one per line)</li>
 *   <li>{@code expected_case_<case>.txt} - Contains expected stdout output</li>
 * </ul>
 *
 * <p>For no-argument tests, use only {@code expected_output.txt} (no commandline_arg files).</p>
 * <p>If no expected files exist, the test only compiles and validates bytecode (no execution).</p>
 *
 * <h2>Example Directory Structure</h2>
 * <pre>
 * switchExpression/
 *   switchExpression.ek9
 *   commandline_arg_1.txt      # contains: 1
 *   expected_case_1.txt        # contains: One\nDone
 *   commandline_arg_2.txt      # contains: 2
 *   expected_case_2.txt        # contains: Two\nDone
 * </pre>
 *
 * <h2>Test Class Example</h2>
 * <pre>{@code
 * class SwitchExpressionTest extends AbstractExecutableBytecodeTest {
 *     public SwitchExpressionTest() {
 *         super("/examples/bytecodeGeneration/switchExpression",
 *             "bytecode.test",
 *             "SwitchExpression",
 *             List.of(new SymbolCountCheck("bytecode.test", 1)));
 *     }
 * }
 * }</pre>
 */
abstract class AbstractExecutableBytecodeTest extends AbstractBytecodeGenerationTest {

  private static final String COMMANDLINE_ARG_PREFIX = "commandline_arg_";
  private static final String EXPECTED_CASE_PREFIX = "expected_case_";
  private static final String EXPECTED_OUTPUT_FILE = "expected_output.txt";

  private final String moduleName;
  private final String programName;

  /**
   * Constructor for executable bytecode tests.
   *
   * @param fromResourcesDirectory Directory containing the .ek9 source file
   * @param moduleName             Module name (e.g., "bytecode.test")
   * @param programName            Program name within the module (e.g., "SwitchExpression")
   * @param expectedSymbols        Symbol count checks for validation
   */
  public AbstractExecutableBytecodeTest(final String fromResourcesDirectory,
                                        final String moduleName,
                                        final String programName,
                                        final List<SymbolCountCheck> expectedSymbols) {
    super(fromResourcesDirectory, expectedSymbols, false, false, false);
    this.moduleName = moduleName;
    this.programName = programName;
  }

  @Override
  protected boolean addDebugInstrumentation() {
    return false;
  }

  @Override
  @Test
  void testPhaseDevelopment() {
    // Clean and compile
    ek9Workspace.getSources().stream().findFirst()
        .ifPresent(source -> fileHandling.cleanEk9DirectoryStructureFor(source.getFileName(), targetArchitecture));

    testToPhase(CompilationPhase.CODE_GENERATION_AGGREGATES);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    // Call parent to handle symbol checks and showBytecode
    super.assertFinalResults(compilationResult, numberOfErrors, program);

    // Execute in-JVM and verify output (if test cases exist)
    try {
      executeAndVerifyOutput();
    } catch (Exception e) {
      throw new AssertionError("In-JVM execution failed", e);
    }
  }

  /**
   * Execute the compiled program in-JVM and verify output against expected files.
   */
  private void executeAndVerifyOutput() throws Exception {
    final var source = ek9Workspace.getSources().stream().findFirst()
        .orElseThrow(() -> new AssertionError("No source found"));

    final var projectDir = new File(source.getFileName()).getParent();
    final var dotEk9Dir = fileHandling.getDotEk9Directory(projectDir);
    final var bytecodeDir = fileHandling.getMainGeneratedOutputDirectory(dotEk9Dir, TargetArchitecture.JVM);
    final var testResourceDir = Path.of(projectDir);

    // Discover test cases
    final var testCases = discoverTestCases(testResourceDir);

    // If no test cases found, skip execution (compile-only test)
    if (testCases.isEmpty()) {
      return;
    }

    // Build fully qualified program class name
    final var programClassName = moduleName + "." + programName;

    int passedCount = 0;
    for (TestCaseConfig testCase : testCases) {
      verifyTestCase(bytecodeDir.toPath(), testResourceDir, programClassName, testCase);
      passedCount++;
    }

    System.out.println("PASS: " + getClass().getSimpleName() + " (" + passedCount + " cases)");
  }

  /**
   * Discover test cases from files in the test directory.
   */
  private List<TestCaseConfig> discoverTestCases(final Path testDir) throws IOException {
    final List<TestCaseConfig> testCases = new ArrayList<>();

    // Find all commandline_arg_*.txt files
    try (Stream<Path> files = Files.list(testDir)) {
      final var argFiles = files
          .filter(p -> p.getFileName().toString().startsWith(COMMANDLINE_ARG_PREFIX))
          .filter(p -> p.getFileName().toString().endsWith(".txt"))
          .toList();

      if (argFiles.isEmpty()) {
        // No arg files - check for expected_output.txt (no-arg single case)
        final var expectedOutput = testDir.resolve(EXPECTED_OUTPUT_FILE);
        if (Files.exists(expectedOutput)) {
          testCases.add(new TestCaseConfig("default", new String[0], expectedOutput));
        }
      } else {
        // Build test cases from arg files
        for (Path argFile : argFiles) {
          final var caseId = extractCaseId(argFile);
          final var args = readArguments(argFile);
          final var expectedFile = testDir.resolve(EXPECTED_CASE_PREFIX + caseId + ".txt");

          if (!Files.exists(expectedFile)) {
            fail("Missing expected file for case '" + caseId + "': " + expectedFile);
          }

          testCases.add(new TestCaseConfig(caseId, args, expectedFile));
        }
      }
    }

    return testCases;
  }

  /**
   * Extract case ID from commandline_arg filename.
   */
  private String extractCaseId(final Path argFile) {
    final var filename = argFile.getFileName().toString();
    return filename.substring(COMMANDLINE_ARG_PREFIX.length(), filename.length() - 4);
  }

  /**
   * Read arguments from commandline_arg file (one argument per line).
   */
  private String[] readArguments(final Path argFile) throws IOException {
    final var lines = Files.readAllLines(argFile);
    return lines.stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toArray(String[]::new);
  }

  /**
   * Execute program with given test case configuration and verify output.
   */
  private void verifyTestCase(final Path bytecodeDir, final Path testResourceDir,
                              final String programClassName, final TestCaseConfig testCase) throws Exception {
    // Execute program - thread-local capture handles parallel safety
    final var actualOutput = BytecodeExecutor.execute(bytecodeDir, programClassName, testCase.args());

    // Read expected output
    final var expectedOutput = Files.readString(testCase.expectedFile());

    // Compare
    assertEquals(expectedOutput, actualOutput,
        "Output mismatch for test case '" + testCase.caseId() + "'");
  }

  /**
   * Test case configuration discovered from files.
   */
  private record TestCaseConfig(String caseId, String[] args, Path expectedFile) {
  }
}
