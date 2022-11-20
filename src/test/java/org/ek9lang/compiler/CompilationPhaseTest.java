package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CompilationPhaseTest {

  @ParameterizedTest
  @ValueSource(strings = {"PARSING",
      "SYMBOL_DEFINITION",
      "REFERENCE_CHECKS",
      "DUPLICATE_CHECKS",
      "SIMPLE_RESOLUTION",
      "TEMPLATE_EXPANSION",
      "FULL_RESOLUTION",
      "PLUGIN_RESOLUTION",
      "SIMPLE_IR_GENERATION",
      "TEMPLATE_IR_GENERATION",
      "IR_ANALYSIS",
      "FUNCTION_CODE_GENERATION",
      "AGGREGATE_CODE_GENERATION",
      "CONSTANT_CODE_GENERATION",
      "APPLICATION_PACKAGING"})
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
