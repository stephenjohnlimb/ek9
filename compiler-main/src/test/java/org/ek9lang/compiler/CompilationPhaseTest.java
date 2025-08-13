package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CompilationPhaseTest {

  @ParameterizedTest
  @ValueSource(strings = {"PARSING",
      "SYMBOL_DEFINITION",
      "DUPLICATION_CHECK",
      "REFERENCE_CHECKS",
      "EXPLICIT_TYPE_SYMBOL_DEFINITION",
      "TYPE_HIERARCHY_CHECKS",
      "FULL_RESOLUTION",
      "PLUGIN_RESOLUTION",
      "IR_GENERATION",
      "IR_ANALYSIS",
      "IR_OPTIMISATION",
      "CODE_GENERATION_PREPARATION",
      "CODE_GENERATION_AGGREGATES",
      "CODE_GENERATION_CONSTANTS",
      "CODE_OPTIMISATION",
      "PLUGIN_LINKAGE",
      "APPLICATION_PACKAGING",
      "PACKAGING_POST_PROCESSING"})
  void testCorrectCreationOfEnumeration(final String fromStringValue) {
    var allowedValue = CompilationPhase.valueOf(fromStringValue);
    assertNotNull(allowedValue);

    //Not really concerned with what the description is, just that there is one.
    assertNotNull(allowedValue.getDescription());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "not_valid", "parsing"})
  void testInvalidCreationOfEnumeration(final String inValidValue) {

    assertThrows(java.lang.IllegalArgumentException.class,
        () -> CompilationPhase.valueOf(inValidValue));
  }
}
