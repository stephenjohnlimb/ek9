package ek9;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for StringToEK9TypeConverter focusing on its BiFunction interface.
 * Validates that it properly implements BiFunction and works in functional contexts.
 */
class StringToEK9TypeConverterTest {

  @Test
  void testBiFunctionInterface() {
    final var converter = new StringToEK9TypeConverter();

    // Test that it works as a BiFunction

    final var result = converter.apply("42", "org.ek9.lang::Integer");
    assertNotNull(result);
    assertInstanceOf(org.ek9.lang.Integer.class, result);

    final var integerResult = (org.ek9.lang.Integer) result;
    assertTrue(integerResult._isSet()._true());
  }

  @Test
  void testApplyMethodCall() {
    final var converter = new StringToEK9TypeConverter();

    final var result = converter.apply("Hello", "org.ek9.lang::String");
    assertNotNull(result);
    assertInstanceOf(org.ek9.lang.String.class, result);

    final var stringResult = (org.ek9.lang.String) result;
    assertTrue(stringResult._isSet()._true());
  }

  @Test
  void testFunctionalComposition() {
    final var converter = new StringToEK9TypeConverter();

    // Test using the converter in a functional context
    final var result = converter.apply("true", "org.ek9.lang::Boolean");
    assertNotNull(result);
    assertInstanceOf(org.ek9.lang.Boolean.class, result);

    final var booleanResult = (org.ek9.lang.Boolean) result;
    assertTrue(booleanResult._isSet()._true());
    assertTrue(booleanResult._true());
  }
}