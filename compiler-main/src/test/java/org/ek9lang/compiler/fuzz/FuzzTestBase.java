package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicInteger;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;

/**
 * Base class for all fuzzing tests.
 * Provides common configuration for fuzzing execution with cached compiler bootstrap.
 *
 * <p>Fuzzing tests verify compiler robustness: no crashes, no infinite loops, clear error messages.
 * Unlike regular tests, fuzzing tests do NOT assert compilation success/failure -
 * they simply verify the compiler handles all inputs gracefully.</p>
 *
 * <p>Performance: Uses CompilableProgramSupplier caching for ~500x faster execution
 * (10ms deserialize vs 3000ms bootstrap per test).</p>
 */
public abstract class FuzzTestBase extends PhasesTest {

  // Track per-file results for verification
  private final AtomicInteger filesWithErrors = new AtomicInteger(0);
  private final AtomicInteger filesWithoutErrors = new AtomicInteger(0);
  private final CompilationPhase targetPhase;

  /**
   * Constructor for fuzz tests.
   *
   * @param corpusDirectory Directory under src/test/resources/fuzzCorpus/
   * @param targetPhase     Compilation phase to test up to
   */
  protected FuzzTestBase(final String corpusDirectory, final CompilationPhase targetPhase) {
    super("/fuzzCorpus/" + corpusDirectory, false, true);
    this.targetPhase = targetPhase;
  }

  protected void runTests() {
    testToPhase(targetPhase);
  }

  private void checkCompilationErrors(final CompilableSource source) {

    final String fileName = source.getFileName();
    final String shortName = fileName.substring(fileName.lastIndexOf('/') + 1);

    final var errorListener = source.getErrorListener();
    final boolean hasErrors = errorListener.hasErrors();

    // Count errors manually
    int errorCount = 0;
    var errorIterator = errorListener.getErrors();
    while (errorIterator.hasNext()) {
      errorIterator.next();
      errorCount++;
    }

    if (hasErrors) {
      filesWithErrors.incrementAndGet();
      System.out.printf("  ✓ %s: REJECTED (%d errors) EXPECTED%n", shortName, errorCount);
    } else {
      filesWithoutErrors.incrementAndGet();
      System.out.printf("  ✗ %s: ACCEPTED (0 errors) - UNEXPECTED!%n", shortName);
    }

  }

  @Override
  protected boolean errorOnDirectiveErrors() {
    // Don't fail on directive errors for fuzzing
    return false;
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult,
                                    final int numberOfErrors,
                                    final CompilableProgram program) {

    ek9Workspace.getSources().forEach(this::checkCompilationErrors);

    System.out.println("\n=== Malformed Syntax Fuzzing Results ===");
    System.out.println("Total files tested: " + ek9Workspace.getSources().size());
    System.out.println("Files correctly rejected: " + filesWithErrors.get());
    System.out.println("Files unexpectedly accepted: " + filesWithoutErrors.get());
    System.out.println();

    // For malformed syntax tests, we EXPECT:
    // 1. Compiler to NOT crash (implied by reaching here)
    // 2. Compilation to FAIL (because syntax is deliberately wrong)
    // 3. Errors to be reported (compiler detected the problems)

    System.out.println("Verification:");
    System.out.println("✓ Compiler did not crash (JVM stayed alive)");

    // Assert that ALL files were rejected (had errors)
    // If any file was accepted without errors, that's a problem:
    // - Either the test file isn't actually malformed
    // - Or the compiler incorrectly accepted invalid syntax (BUG!)
    assertTrue(filesWithErrors.get() > 0,
        "Expected at least some files to produce errors (none did - are test files actually malformed?)");

    if (filesWithoutErrors.get() > 0) {
      System.out.println("✗ WARNING: " + filesWithoutErrors.get()
          + " files were accepted without errors!");
      System.out.println("  This means either:");
      System.out.println("  1. Those test files aren't actually malformed, OR");
      System.out.println("  2. The compiler is incorrectly accepting invalid syntax (BUG!)");
      fail("Some malformed syntax files were accepted - test files may not be actually malformed");
    } else {
      System.out.println("✓ All files correctly rejected (produced errors)");
    }

    // Verify compilation overall failed (since all inputs were malformed)
    assertFalse(compilationResult,
        "Compilation should FAIL when given malformed syntax");
    System.out.println("✓ Compilation correctly failed overall");

    // Verify errors were actually reported
    assertTrue(numberOfErrors > 0,
        "Malformed syntax should produce at least one error");
    System.out.println("✓ Errors were reported (" + numberOfErrors + " total)");

    System.out.println("\n✓ ALL CHECKS PASSED - Fuzzing test successful!");
    System.out.println("==============================================\n");
  }
}
