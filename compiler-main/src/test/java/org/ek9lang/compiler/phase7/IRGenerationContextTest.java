package org.ek9lang.compiler.phase7;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.core.SharedThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Unit tests for IRGenerationContext in isolation.
 * Tests all name generation methods, thread safety, and state management.
 */
@Execution(SAME_THREAD)
@ResourceLock(value = "file_access", mode = READ_WRITE)
class IRGenerationContextTest {

  private ParsedModule mockParsedModule;
  private CompilerFlags mockCompilerFlags;
  private IRContext underTest;

  @BeforeEach
  void setUp() {
    // Create a simple mock ParsedModule without external dependencies
    mockParsedModule = createMockParsedModule();
    mockCompilerFlags = new CompilerFlags();
    underTest = new IRContext(mockParsedModule, mockCompilerFlags);
  }

  /**
   * Create a simple test ParsedModule for testing purposes.
   * Uses minimal implementation to avoid external dependencies.
   */
  private ParsedModule createMockParsedModule() {
    try {
      // For this test, we only need a non-null ParsedModule instance.
      // IRGenerationContext only uses it to store the reference and return it via getParsedModule().

      // Create a temporary file for the test
      File tempFile = Files.createTempFile("ir-generation-context-test", ".ek9").toFile();
      Files.write(tempFile.toPath(), "//Test content for IRGenerationContext".getBytes());
      tempFile.deleteOnExit(); // Clean up after test

      // Create CompilableSource and SharedThreadContext
      var source = new CompilableSource(tempFile.getParent(), tempFile.getAbsolutePath());
      var sharedContext = new SharedThreadContext<>(new CompilableProgram());

      return new ParsedModule(source, sharedContext);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create test ParsedModule", e);
    }
  }

  // ================== Constructor and Basic Setup Tests ==================

  @Test
  void testConstructorWithValidParsedModule() {
    var context = new IRContext(mockParsedModule, mockCompilerFlags);
    assertNotNull(context);
    assertEquals(mockParsedModule, context.getParsedModule());
  }

  @Test
  void testConstructorWithNullParsedModuleThrowsException() {
    var exception = assertThrows(IllegalArgumentException.class,
        () -> new IRContext(null, mockCompilerFlags));
    assertTrue(exception.getMessage().contains("ParsedModule cannot be null"));
  }

  @Test
  void testConstructorWithNullCompilerFlagsThrowsException() {
    var exception = assertThrows(IllegalArgumentException.class,
        () -> new IRContext(mockParsedModule, null));
    assertTrue(exception.getMessage().contains("CompilerFlags cannot be null"));
  }

  @Test
  void testGetParsedModuleReturnsInjectedModule() {
    assertEquals(mockParsedModule, underTest.getParsedModule());
  }

  // ================== Temporary Variable Name Generation Tests ==================

  @Test
  void testGenerateTempNameUniqueness() {
    Set<String> generatedNames = new HashSet<>();

    // Generate 100 temp names and verify all unique
    for (int i = 0; i < 100; i++) {
      String tempName = underTest.generateTempName();
      assertTrue(generatedNames.add(tempName),
          "Duplicate temp name generated: " + tempName);
    }

    assertEquals(100, generatedNames.size());
  }

  @Test
  void testGenerateTempNameFormat() {
    Pattern expectedPattern = Pattern.compile("^_temp\\d+$");

    // Test first few generated names match expected format
    for (int i = 0; i < 5; i++) {
      String tempName = underTest.generateTempName();
      assertTrue(expectedPattern.matcher(tempName).matches(),
          "Temp name doesn't match expected format: " + tempName);
    }
  }

  @Test
  void testGenerateTempNameSequential() {
    // Verify sequential numbering
    assertEquals("_temp1", underTest.generateTempName());
    assertEquals("_temp2", underTest.generateTempName());
    assertEquals("_temp3", underTest.generateTempName());
  }

  @Test
  void testTempCounterStartsAtZero() {
    assertEquals(0, underTest.getTempCounter());
  }

  @Test
  void testTempCounterIncrementsCorrectly() {
    assertEquals(0, underTest.getTempCounter());

    underTest.generateTempName();
    assertEquals(1, underTest.getTempCounter());

    underTest.generateTempName();
    assertEquals(2, underTest.getTempCounter());

    underTest.generateTempName();
    assertEquals(3, underTest.getTempCounter());
  }

  @Test
  void testGenerateTempNameThreadSafety() throws InterruptedException {
    Set<String> allGeneratedNames = ConcurrentHashMap.newKeySet();

    try (final var executor = Executors.newFixedThreadPool(10)) {

      // Generate temp names concurrently from 10 threads
      for (int thread = 0; thread < 10; thread++) {
        executor.submit(() -> {
          for (int i = 0; i < 10; i++) {
            String tempName = underTest.generateTempName();
            allGeneratedNames.add(tempName);
          }
        });
      }

      executor.shutdown();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    // All 100 names should be unique
    assertEquals(100, allGeneratedNames.size());
  }

  // ================== Scope ID Generation Tests ==================

  @Test
  void testGenerateScopeIdWithPrefix() {
    assertEquals("_main_1", underTest.generateScopeId("main"));
    assertEquals("_if_1", underTest.generateScopeId("if"));    // Each prefix starts at 1
    assertEquals("_loop_1", underTest.generateScopeId("loop")); // Each prefix starts at 1
    assertEquals("_main_2", underTest.generateScopeId("main")); // Same prefix increments
  }

  @Test
  void testGenerateScopeIdUniqueness() {
    Set<String> generatedIds = new HashSet<>();
    String[] prefixes = {"main", "if", "loop", "try", "catch"};

    // Generate 20 scope IDs with various prefixes
    for (int i = 0; i < 20; i++) {
      String prefix = prefixes[i % prefixes.length];
      String scopeId = underTest.generateScopeId(prefix);
      assertTrue(generatedIds.add(scopeId),
          "Duplicate scope ID generated: " + scopeId);
    }

    assertEquals(20, generatedIds.size());
  }

  @Test
  void testGenerateScopeIdFormat() {
    Pattern expectedPattern = Pattern.compile("^_\\w+_\\d+$");

    String[] prefixes = {"main", "if", "while", "for", "try"};
    for (String prefix : prefixes) {
      String scopeId = underTest.generateScopeId(prefix);
      assertTrue(expectedPattern.matcher(scopeId).matches(),
          "Scope ID doesn't match expected format: " + scopeId);
    }
  }

  @Test
  void testScopeCounterProgression() {
    assertEquals(0, underTest.getCounterFor("main"));
    assertEquals(0, underTest.getCounterFor("if"));

    underTest.generateScopeId("main");
    assertEquals(1, underTest.getCounterFor("main"));
    assertEquals(0, underTest.getCounterFor("if"));

    underTest.generateScopeId("if");
    assertEquals(1, underTest.getCounterFor("main"));
    assertEquals(1, underTest.getCounterFor("if"));
    
    // Test that same prefix increments independently
    underTest.generateScopeId("main");
    assertEquals(2, underTest.getCounterFor("main"));
    assertEquals(1, underTest.getCounterFor("if"));
  }

  @Test
  void testGenerateScopeIdWithEmptyPrefix() {
    String scopeId = underTest.generateScopeId("");
    assertEquals("__1", scopeId);  // "_" + "" + "_" + 1
  }

  // ================== Block Label Generation Tests ==================

  @Test
  void testGenerateBlockLabelWithPrefix() {
    assertEquals("_entry_1", underTest.generateBlockLabel("entry"));
    assertEquals("_if_then_1", underTest.generateBlockLabel("if_then"));  // Each prefix starts at 1
    assertEquals("_loop_body_1", underTest.generateBlockLabel("loop_body")); // Each prefix starts at 1
    assertEquals("_entry_2", underTest.generateBlockLabel("entry"));      // Same prefix increments
  }

  @Test
  void testGenerateBlockLabelUniqueness() {
    Set<String> generatedLabels = new HashSet<>();
    String[] prefixes = {"entry", "if_then", "if_else", "loop", "exit"};

    // Generate 25 block labels with various prefixes
    for (int i = 0; i < 25; i++) {
      String prefix = prefixes[i % prefixes.length];
      String blockLabel = underTest.generateBlockLabel(prefix);
      assertTrue(generatedLabels.add(blockLabel),
          "Duplicate block label generated: " + blockLabel);
    }

    assertEquals(25, generatedLabels.size());
  }

  @Test
  void testGenerateBlockLabelFormat() {
    Pattern expectedPattern = Pattern.compile("^_[\\w_]+_\\d+$");

    String[] prefixes = {"entry", "if_then", "loop_body", "catch_handler"};
    for (String prefix : prefixes) {
      String blockLabel = underTest.generateBlockLabel(prefix);
      assertTrue(expectedPattern.matcher(blockLabel).matches(),
          "Block label doesn't match expected format: " + blockLabel);
    }
  }

  @Test
  void testBlockCounterProgression() {
    assertEquals(0, underTest.getCounterFor("entry"));
    assertEquals(0, underTest.getCounterFor("if_then"));

    underTest.generateBlockLabel("entry");
    assertEquals(1, underTest.getCounterFor("entry"));
    assertEquals(0, underTest.getCounterFor("if_then"));

    underTest.generateBlockLabel("if_then");
    assertEquals(1, underTest.getCounterFor("entry"));
    assertEquals(1, underTest.getCounterFor("if_then"));
    
    // Test that same prefix increments independently
    underTest.generateBlockLabel("entry");
    assertEquals(2, underTest.getCounterFor("entry"));
    assertEquals(1, underTest.getCounterFor("if_then"));
  }

  // ================== General Label Generation Tests ==================

  @Test
  void testGenerateLabelNameWithPrefix() {
    assertEquals("_var1_unset_1", underTest.generateLabelName("var1_unset"));
    assertEquals("_end_label_1", underTest.generateLabelName("end_label"));  // Each prefix starts at 1
    assertEquals("_continue_1", underTest.generateLabelName("continue"));    // Each prefix starts at 1
    assertEquals("_var1_unset_2", underTest.generateLabelName("var1_unset")); // Same prefix increments
  }

  @Test
  void testGenerateLabelNameUniqueness() {
    Set<String> generatedLabels = new HashSet<>();
    String[] prefixes = {"var_unset", "end_label", "continue", "break", "return"};

    // Generate 30 labels with various prefixes
    for (int i = 0; i < 30; i++) {
      String prefix = prefixes[i % prefixes.length];
      String label = underTest.generateLabelName(prefix);
      assertTrue(generatedLabels.add(label),
          "Duplicate label generated: " + label);
    }

    assertEquals(30, generatedLabels.size());
  }

  @Test
  void testGenerateLabelNameFormat() {
    Pattern expectedPattern = Pattern.compile("^_[\\w_]+_\\d+$");

    String[] prefixes = {"var_unset", "end_label", "continue_point", "exception_handler"};
    for (String prefix : prefixes) {
      String label = underTest.generateLabelName(prefix);
      assertTrue(expectedPattern.matcher(label).matches(),
          "Label doesn't match expected format: " + label);
    }
  }

  // ================== Counter State Tests ==================

  @Test
  void testGetTempCounterInitialValue() {
    assertEquals(0, underTest.getTempCounter());
  }

  @Test
  void testGetCounterForUnusedPrefix() {
    assertEquals(0, underTest.getCounterFor("unused"));
  }

  @Test
  void testUniquePrefixCount() {
    assertEquals(0, underTest.getUniquePrefixCount());
    
    underTest.generateScopeId("test");
    assertEquals(1, underTest.getUniquePrefixCount());
    
    underTest.generateBlockLabel("block");
    assertEquals(2, underTest.getUniquePrefixCount());
  }

  @Test
  void testCountersIndependentIncrement() {
    // Generate one of each type with different prefixes
    underTest.generateTempName();         // temp counter = 1
    underTest.generateScopeId("scope");   // scope counter = 1  
    underTest.generateBlockLabel("block"); // block counter = 1
    underTest.generateLabelName("label"); // label counter = 1

    // Verify counters are independent per prefix
    assertEquals(1, underTest.getTempCounter());
    assertEquals(1, underTest.getCounterFor("scope"));
    assertEquals(1, underTest.getCounterFor("block"));
    assertEquals(1, underTest.getCounterFor("label"));

    // Generate more temps, verify others don't change
    underTest.generateTempName();
    underTest.generateTempName();

    assertEquals(3, underTest.getTempCounter());
    assertEquals(1, underTest.getCounterFor("scope"));  // Unchanged
    assertEquals(1, underTest.getCounterFor("block"));  // Unchanged
    assertEquals(1, underTest.getCounterFor("label"));  // Unchanged
    
    // Generate more with same prefix, verify independent incrementing
    underTest.generateScopeId("scope");
    assertEquals(3, underTest.getTempCounter());        // Unchanged
    assertEquals(2, underTest.getCounterFor("scope"));  // Incremented
    assertEquals(1, underTest.getCounterFor("block"));  // Unchanged
  }

  // ================== Integration and Edge Case Tests ==================

  @Test
  void testMultipleGenerationMethodsIndependence() {
    // Generate various names and verify all are unique globally
    Set<String> allGeneratedNames = new HashSet<>();

    // Generate different types of names
    for (int i = 0; i < 5; i++) {
      allGeneratedNames.add(underTest.generateTempName());
      allGeneratedNames.add(underTest.generateScopeId("scope"));
      allGeneratedNames.add(underTest.generateBlockLabel("block"));
      allGeneratedNames.add(underTest.generateLabelName("label"));
    }

    // All names should be unique across all generation methods
    assertEquals(20, allGeneratedNames.size());
  }

  @Test
  void testLargeNumberGeneration() {
    // Test with high counter values to ensure no overflow issues
    Set<String> tempNames = new HashSet<>();

    for (int i = 0; i < 1000; i++) {
      tempNames.add(underTest.generateTempName());
    }

    assertEquals(1000, tempNames.size());
    assertEquals(1000, underTest.getTempCounter());
  }

  @Test
  void testSpecialCharacterPrefixes() {
    // Test prefixes with underscores and alphanumeric characters
    String scopeId1 = underTest.generateScopeId("test_prefix");
    String scopeId2 = underTest.generateScopeId("prefix123");
    String scopeId3 = underTest.generateScopeId("a_b_c");

    assertEquals("_test_prefix_1", scopeId1);
    assertEquals("_prefix123_1", scopeId2);   // Each prefix starts at 1
    assertEquals("_a_b_c_1", scopeId3);       // Each prefix starts at 1
    
    // Test same prefix increments
    String scopeId4 = underTest.generateScopeId("prefix123");
    assertEquals("_prefix123_2", scopeId4);   // Second use of same prefix
  }
}