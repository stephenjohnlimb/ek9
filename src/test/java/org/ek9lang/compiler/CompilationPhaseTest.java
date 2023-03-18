package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CompilationPhaseTest {

  @ParameterizedTest
  @ValueSource(strings = {"PREPARE_PARSE",
      "PARSING",
      "SYMBOL_DEFINITION",
      "DUPLICATION_CHECK",
      "REFERENCE_CHECKS",
      "DUPLICATE_CHECKS",
      "EXPLICIT_TYPE_SYMBOL_DEFINITION",
      "TEMPLATE_DEFINITION_RESOLUTION",
      "FURTHER_SYMBOL_DEFINITION",
      "TEMPLATE_EXPANSION",
      "FULL_RESOLUTION",
      "PLUGIN_RESOLUTION",
      "SIMPLE_IR_GENERATION",
      "PROGRAM_IR_CONFIGURATION",
      "TEMPLATE_IR_GENERATION",
      "IR_ANALYSIS",
      "IR_OPTIMISATION",
      "CODE_GENERATION_PREPARATION",
      "CODE_GENERATION_AGGREGATES",
      "CODE_GENERATION_CONSTANTS",
      "CODE_GENERATION_FUNCTIONS",
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
