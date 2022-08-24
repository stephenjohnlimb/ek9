package org.ek9lang.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

/**
 * Simple tests for the compiler flags in general use and the lsp specific flags.
 */
class Ek9CompilerConfigTest {

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
