package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for the OptimizationLevel enum.
 */
class OptimizationLevelTest {

  @Test
  void testAllLevelsExist() {
    assertEquals(3, OptimizationLevel.values().length);
    assertEquals(OptimizationLevel.O0, OptimizationLevel.valueOf("O0"));
    assertEquals(OptimizationLevel.O2, OptimizationLevel.valueOf("O2"));
    assertEquals(OptimizationLevel.O3, OptimizationLevel.valueOf("O3"));
  }

  @Test
  void testFromStringLowercase() {
    assertEquals(OptimizationLevel.O0, OptimizationLevel.from("o0"));
    assertEquals(OptimizationLevel.O2, OptimizationLevel.from("o2"));
    assertEquals(OptimizationLevel.O3, OptimizationLevel.from("o3"));
  }

  @Test
  void testFromStringUppercase() {
    assertEquals(OptimizationLevel.O0, OptimizationLevel.from("O0"));
    assertEquals(OptimizationLevel.O2, OptimizationLevel.from("O2"));
    assertEquals(OptimizationLevel.O3, OptimizationLevel.from("O3"));
  }

  @Test
  void testFromStringMixedCase() {
    assertEquals(OptimizationLevel.O0, OptimizationLevel.from("o0"));
    assertEquals(OptimizationLevel.O2, OptimizationLevel.from("O2"));
    assertEquals(OptimizationLevel.O3, OptimizationLevel.from("o3"));
  }

  @Test
  void testInvalidInputDefaultsToO2() {
    assertEquals(OptimizationLevel.O2, OptimizationLevel.from("invalid"));
    assertEquals(OptimizationLevel.O2, OptimizationLevel.from("O1"));
    assertEquals(OptimizationLevel.O2, OptimizationLevel.from(""));
    assertEquals(OptimizationLevel.O2, OptimizationLevel.from("O99"));
  }

  @Test
  void testGetDescription() {
    assertEquals("o0", OptimizationLevel.O0.getDescription());
    assertEquals("o2", OptimizationLevel.O2.getDescription());
    assertEquals("o3", OptimizationLevel.O3.getDescription());
  }
}
