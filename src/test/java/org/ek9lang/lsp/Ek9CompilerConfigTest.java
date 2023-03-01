package org.ek9lang.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Simple tests for the compiler flags in general use and the lsp specific flags.
 */
final class Ek9CompilerConfigTest {

  @Test
  void testDefaultToFullPackaging() {
    var underTest = new Ek9CompilerConfig();
    assertEquals(CompilationPhase.APPLICATION_PACKAGING, underTest.getCompileToPhase());
  }

  @Test
  void testBasicDefaultValues() {
    var underTest = new Ek9CompilerConfig();

    assertTrue(underTest.isProvideLanguageHoverHelp());
    assertTrue(underTest.isSuggestionRequired());
    assertEquals(5, underTest.getNumberOfSuggestions());
  }

  @Test
  void testConfigurationCanBeAltered() {
    var underTest = new Ek9CompilerConfig();
    underTest.setNumberOfSuggestions(10);

    assertTrue(underTest.isSuggestionRequired());
    assertEquals(10, underTest.getNumberOfSuggestions());
  }

  @Test
  void testSettingsCanBeSwitchedOff() {
    var underTest = new Ek9CompilerConfig();
    underTest.setNumberOfSuggestions(0);
    underTest.setProvideLanguageHoverHelp(false);

    assertFalse(underTest.isProvideLanguageHoverHelp());
    assertFalse(underTest.isSuggestionRequired());
    assertEquals(0, underTest.getNumberOfSuggestions());
  }
}
