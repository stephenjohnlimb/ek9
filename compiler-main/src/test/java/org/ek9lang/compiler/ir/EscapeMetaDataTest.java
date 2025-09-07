package org.ek9lang.compiler.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for EscapeMetaData record and IR instruction integration.
 */
class EscapeMetaDataTest {

  @Test
  void testEscapeMetaDataCreation() {
    // Test basic creation
    final var metadata = new EscapeMetaData(
        EscapeMetaData.EscapeLevel.NONE,
        EscapeMetaData.LifetimeScope.LOCAL_SCOPE,
        Set.of("STACK_CANDIDATE")
    );
    
    assertEquals(EscapeMetaData.EscapeLevel.NONE, metadata.escapeLevel());
    assertEquals(EscapeMetaData.LifetimeScope.LOCAL_SCOPE, metadata.lifetimeScope());
    assertTrue(metadata.optimizationHints().contains("STACK_CANDIDATE"));
  }

  @Test
  void testEscapeMetaDataFactoryMethods() {
    // Test noEscape factory method
    final var noEscape = EscapeMetaData.noEscape(EscapeMetaData.LifetimeScope.FUNCTION);
    assertEquals(EscapeMetaData.EscapeLevel.NONE, noEscape.escapeLevel());
    assertEquals(EscapeMetaData.LifetimeScope.FUNCTION, noEscape.lifetimeScope());
    assertTrue(noEscape.optimizationHints().contains("STACK_CANDIDATE"));

    // Test escapeParameter factory method
    final var escapeParam = EscapeMetaData.escapeParameter(EscapeMetaData.LifetimeScope.FUNCTION);
    assertEquals(EscapeMetaData.EscapeLevel.PARAMETER, escapeParam.escapeLevel());
    assertTrue(escapeParam.optimizationHints().isEmpty());

    // Test escapeGlobal factory method
    final var escapeGlobal = EscapeMetaData.escapeGlobal(EscapeMetaData.LifetimeScope.STATIC);
    assertEquals(EscapeMetaData.EscapeLevel.GLOBAL, escapeGlobal.escapeLevel());
    assertEquals(EscapeMetaData.LifetimeScope.STATIC, escapeGlobal.lifetimeScope());
  }

  @Test
  void testEscapeMetaDataStringification() {
    // Test basic stringification
    final var metadata = EscapeMetaData.noEscape(EscapeMetaData.LifetimeScope.LOCAL_SCOPE);
    final var expected = "[escape=NONE, lifetime=LOCAL_SCOPE, hints=STACK_CANDIDATE]";
    assertEquals(expected, metadata.toString());

    // Test with unknown lifetime
    final var unknownLifetime = new EscapeMetaData(
        EscapeMetaData.EscapeLevel.GLOBAL,
        EscapeMetaData.LifetimeScope.UNKNOWN,
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
    final var metadata = EscapeMetaData.noEscape(EscapeMetaData.LifetimeScope.LOCAL_SCOPE);
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
    final var metadata = EscapeMetaData.noEscape(EscapeMetaData.LifetimeScope.LOCAL_SCOPE);
    instr.setEscapeMetaData(metadata);
    
    final var withMetadata = instr.toString();
    final var expected = "_temp1 = LOAD variable [escape=NONE, lifetime=LOCAL_SCOPE, hints=STACK_CANDIDATE]";
    assertEquals(expected, withMetadata);
  }
}