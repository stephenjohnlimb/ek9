package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for CompilerFlags configuration.
 */
class CompilerFlagsTest {

  @Test
  void testDefaultOptimizationLevel() {
    final var flags = new CompilerFlags();
    assertEquals(OptimizationLevel.O2, flags.getOptimizationLevel());
  }

  @Test
  void testSetOptimizationLevel() {
    final var flags = new CompilerFlags();
    flags.setOptimizationLevel(OptimizationLevel.O0);
    assertEquals(OptimizationLevel.O0, flags.getOptimizationLevel());

    flags.setOptimizationLevel(OptimizationLevel.O3);
    assertEquals(OptimizationLevel.O3, flags.getOptimizationLevel());
  }

  @Test
  void testDefaultCompileToPhase() {
    final var flags = new CompilerFlags();
    assertEquals(CompilationPhase.APPLICATION_PACKAGING, flags.getCompileToPhase());
  }

  @Test
  void testSetCompileToPhase() {
    final var flags = new CompilerFlags();
    flags.setCompileToPhase(CompilationPhase.IR_ANALYSIS);
    assertEquals(CompilationPhase.IR_ANALYSIS, flags.getCompileToPhase());
    assertTrue(flags.isCheckCompilationOnly());
  }

  @Test
  void testVerboseFlag() {
    final var flags = new CompilerFlags(false);
    assertFalse(flags.isVerbose());

    final var verboseFlags = new CompilerFlags(true);
    assertTrue(verboseFlags.isVerbose());
  }

  @Test
  void testDebuggingInstrumentation() {
    final var flags = new CompilerFlags();
    assertFalse(flags.isDebuggingInstrumentation());

    flags.setDebuggingInstrumentation(true);
    assertTrue(flags.isDebuggingInstrumentation());
  }

  @Test
  void testDevBuild() {
    final var flags = new CompilerFlags();
    assertFalse(flags.isDevBuild());

    flags.setDevBuild(true);
    assertTrue(flags.isDevBuild());
  }

  @Test
  void testCheckCompilationOnly() {
    final var flags = new CompilerFlags();
    assertFalse(flags.isCheckCompilationOnly());

    flags.setCheckCompilationOnly(true);
    assertTrue(flags.isCheckCompilationOnly());
    assertEquals(CompilationPhase.IR_ANALYSIS, flags.getCompileToPhase());
  }

  @Test
  void testSuggestionConfiguration() {
    final var flags = new CompilerFlags();
    assertTrue(flags.isSuggestionRequired());
    assertEquals(5, flags.getNumberOfSuggestions());

    flags.setNumberOfSuggestions(10);
    assertEquals(10, flags.getNumberOfSuggestions());

    flags.setNumberOfSuggestions(0);
    assertFalse(flags.isSuggestionRequired());
  }
}
