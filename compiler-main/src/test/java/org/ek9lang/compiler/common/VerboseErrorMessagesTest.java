package org.ek9lang.compiler.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for VerboseErrorMessages class.
 * Validates the verbose error messaging system used for AI-assisted development.
 */
class VerboseErrorMessagesTest {

  @BeforeEach
  void setUp() {
    VerboseErrorMessages.setVerboseEnabled(false);
  }

  @AfterEach
  void tearDown() {
    VerboseErrorMessages.setVerboseEnabled(false);
  }

  @Test
  void testVerboseEnabledFlagDefaultsToFalse() {
    assertFalse(VerboseErrorMessages.isVerboseEnabled());
  }

  @Test
  void testVerboseEnabledFlagCanBeSet() {
    assertFalse(VerboseErrorMessages.isVerboseEnabled());
    VerboseErrorMessages.setVerboseEnabled(true);
    assertTrue(VerboseErrorMessages.isVerboseEnabled());
  }

  @Test
  void testGetVerboseMessageForCommonErrorCode() {
    final var message = VerboseErrorMessages.getVerboseMessage("E50060");
    assertNotNull(message);
    assertTrue(message.contains("COMMON CAUSES"));
    assertTrue(message.contains("Method name typo"));
  }

  @Test
  void testGetVerboseMessageForTypeNotResolved() {
    final var message = VerboseErrorMessages.getVerboseMessage("E50010");
    assertNotNull(message);
    assertTrue(message.contains("Type name misspelled"));
  }

  @Test
  void testGetVerboseMessageForNotResolved() {
    final var message = VerboseErrorMessages.getVerboseMessage("E50001");
    assertNotNull(message);
    assertTrue(message.contains("DISTINCTION"));
    assertTrue(message.contains("E50010"));
  }

  @Test
  void testGetVerboseMessageForUnknownErrorCode() {
    final var message = VerboseErrorMessages.getVerboseMessage("E99999");
    assertNull(message);
  }

  @Test
  void testAllMajorErrorCodesHaveVerboseMessages() {
    // Test a sampling of error codes from different phases
    final var errorCodes = new String[] {
        // Phase 01
        "E01010", "E01020", "E01030", "E01040", "E01050",
        // Phase 02
        "E02010", "E02030", "E02050",
        // Phase 03
        "E03010", "E03020", "E03030",
        // Phase 04
        "E04010", "E04030", "E04060", "E04080",
        // Phase 05
        "E05020", "E05030", "E05100", "E05110",
        // Phase 06
        "E06010", "E06020", "E06140", "E06180",
        // Phase 07
        "E07010", "E07100", "E07310", "E07620",
        // Phase 08
        "E08010", "E08020", "E08100", "E08130",
        // Common (E50xxx)
        "E50001", "E50010", "E50030", "E50060"
    };

    for (String errorCode : errorCodes) {
      final var message = VerboseErrorMessages.getVerboseMessage(errorCode);
      assertNotNull(message, "Missing verbose message for " + errorCode);
      assertTrue(message.contains("COMMON CAUSES") || message.contains("TO FIX"),
          "Verbose message for " + errorCode + " should contain guidance");
    }
  }

  @Test
  void testVerboseMessageFormatContainsExpectedSections() {
    final var message = VerboseErrorMessages.getVerboseMessage("E50060");
    assertNotNull(message);
    // Should have structured content
    assertTrue(message.contains("COMMON CAUSES:"));
    assertTrue(message.contains("TO FIX:") || message.contains("DISTINCTION:"));
  }

  @Test
  void testDirectiveErrorCodesHaveVerboseMessages() {
    final var directiveErrorCodes = new String[] {
        "E50200", "E50210", "E50220", "E50230", "E50240",
        "E50250", "E50260", "E50270", "E50280", "E50290",
        "E50300", "E50310"
    };

    for (String errorCode : directiveErrorCodes) {
      final var message = VerboseErrorMessages.getVerboseMessage(errorCode);
      assertNotNull(message, "Missing verbose message for directive error " + errorCode);
    }
  }
}
