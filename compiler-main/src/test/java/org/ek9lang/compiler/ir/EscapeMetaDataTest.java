package org.ek9lang.compiler.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.ek9lang.compiler.ir.data.EscapeMetaDataDetails;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.junit.jupiter.api.Test;

/**
 * Tests for EscapeMetaDataDetails record and IR instruction integration.
 */
class EscapeMetaDataTest {

  @Test
  void testEscapeMetaDataCreation() {
    // Test basic creation
    final var metadata = new EscapeMetaDataDetails(
        EscapeMetaDataDetails.EscapeLevel.NONE,
        EscapeMetaDataDetails.LifetimeScope.LOCAL_SCOPE,
        Set.of("STACK_CANDIDATE")
    );
    
    assertEquals(EscapeMetaDataDetails.EscapeLevel.NONE, metadata.escapeLevel());
    assertEquals(EscapeMetaDataDetails.LifetimeScope.LOCAL_SCOPE, metadata.lifetimeScope());
    assertTrue(metadata.optimizationHints().contains("STACK_CANDIDATE"));
  }

  @Test
  void testEscapeMetaDataFactoryMethods() {
    // Test noEscape factory method
    final var noEscape = EscapeMetaDataDetails.noEscape(EscapeMetaDataDetails.LifetimeScope.FUNCTION);
    assertEquals(EscapeMetaDataDetails.EscapeLevel.NONE, noEscape.escapeLevel());
    assertEquals(EscapeMetaDataDetails.LifetimeScope.FUNCTION, noEscape.lifetimeScope());
    assertTrue(noEscape.optimizationHints().contains("STACK_CANDIDATE"));

    // Test escapeParameter factory method
    final var escapeParam = EscapeMetaDataDetails.escapeParameter(EscapeMetaDataDetails.LifetimeScope.FUNCTION);
    assertEquals(EscapeMetaDataDetails.EscapeLevel.PARAMETER, escapeParam.escapeLevel());
    assertTrue(escapeParam.optimizationHints().isEmpty());

    // Test escapeGlobal factory method
    final var escapeGlobal = EscapeMetaDataDetails.escapeGlobal(EscapeMetaDataDetails.LifetimeScope.STATIC);
    assertEquals(EscapeMetaDataDetails.EscapeLevel.GLOBAL, escapeGlobal.escapeLevel());
    assertEquals(EscapeMetaDataDetails.LifetimeScope.STATIC, escapeGlobal.lifetimeScope());
  }

  @Test
  void testEscapeMetaDataStringification() {
    // Test basic stringification
    final var metadata = EscapeMetaDataDetails.noEscape(EscapeMetaDataDetails.LifetimeScope.LOCAL_SCOPE);
    final var expected = "[escape=NONE, lifetime=LOCAL_SCOPE, hints=STACK_CANDIDATE]";
    assertEquals(expected, metadata.toString());

    // Test with unknown lifetime
    final var unknownLifetime = new EscapeMetaDataDetails(
        EscapeMetaDataDetails.EscapeLevel.GLOBAL,
        EscapeMetaDataDetails.LifetimeScope.UNKNOWN,
        Set.of()
    );
    assertEquals("[escape=GLOBAL]", unknownLifetime.toString());
  }

  @Test
  void testIRInstructionEscapeMetaDataIntegration() {
    // Test IRInstr with escape metadata
    final var instr = MemoryInstr.load("_temp1", "variable");
    
    // Initially no metadata
    assertFalse(instr.hasEscapeMetaData());
    assertTrue(instr.getEscapeMetaData().isEmpty());

    // Add metadata
    final var metadata = EscapeMetaDataDetails.noEscape(EscapeMetaDataDetails.LifetimeScope.LOCAL_SCOPE);
    instr.setEscapeMetaData(metadata);
    
    // Verify metadata is present
    assertTrue(instr.hasEscapeMetaData());
    assertTrue(instr.getEscapeMetaData().isPresent());
    assertEquals(metadata, instr.getEscapeMetaData().get());
  }

  @Test
  void testIRInstructionStringificationWithMetaData() {
    // Test that IR instruction toString includes escape metadata
    final var instr = MemoryInstr.load("_temp1", "variable");
    
    // Without metadata
    final var withoutMetadata = instr.toString();
    assertEquals("_temp1 = LOAD variable", withoutMetadata);
    
    // With metadata
    final var metadata = EscapeMetaDataDetails.noEscape(EscapeMetaDataDetails.LifetimeScope.LOCAL_SCOPE);
    instr.setEscapeMetaData(metadata);
    
    final var withMetadata = instr.toString();
    final var expected = "_temp1 = LOAD variable [escape=NONE, lifetime=LOCAL_SCOPE, hints=STACK_CANDIDATE]";
    assertEquals(expected, withMetadata);
  }
}