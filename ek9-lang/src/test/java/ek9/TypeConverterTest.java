package ek9;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ek9.TypeConverter.TypeConversionException;
import java.util.stream.Stream;
import org.ek9.lang.BuiltinType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for TypeConverter following EK9's tri-state semantics.
 * Tests that valid conversions return set objects and invalid conversions return unset objects.
 */
class TypeConverterTest {

  private final TypeConverter typeConverter = new TypeConverter();

  @Test
  void testStringConversionsAlwaysSucceed() {
    // String conversions should always succeed and be set, including empty strings
    assertValidConversion("Hello, World", "org.ek9.lang::String", org.ek9.lang.String.class);
    assertValidConversion("", "org.ek9.lang::String", org.ek9.lang.String.class);
    assertValidConversion("   ", "org.ek9.lang::String", org.ek9.lang.String.class);
  }

  @ParameterizedTest
  @MethodSource("validConversionData")
  void testValidConversions(final String value, final String typeName, final Class<?> expectedClass) {
    assertValidConversion(value, typeName, expectedClass);
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "TRUE", "True"})
  void testTrueBooleanConversions(final String value) {
    final var result = typeConverter.convertToEK9Type(value, "org.ek9.lang::Boolean");
    assertNotNull(result);
    assertInstanceOf(org.ek9.lang.Boolean.class, result);

    final var booleanResult = (org.ek9.lang.Boolean) result;
    assertTrue(booleanResult._true());
  }

  @ParameterizedTest
  @ValueSource(strings = {"false", "FALSE", "False"})
  void testFalseBooleanConversions(final String value) {
    final var result = typeConverter.convertToEK9Type(value, "org.ek9.lang::Boolean");
    assertNotNull(result);
    assertInstanceOf(org.ek9.lang.Boolean.class, result);

    final var booleanResult = (org.ek9.lang.Boolean) result;
    assertTrue(booleanResult._false());
  }

  @ParameterizedTest
  @ValueSource(strings = {"yes", "no", "1", "0", "maybe"})
  void testInvalidBooleanConversionsReturnFalse(final String value) {
    final var result = typeConverter.convertToEK9Type(value, "org.ek9.lang::Boolean");
    assertNotNull(result);
    assertInstanceOf(org.ek9.lang.Boolean.class, result);

    final var booleanResult = (org.ek9.lang.Boolean) result;
    assertTrue(booleanResult._isSet()._false());
  }

  @ParameterizedTest
  @MethodSource("invalidConversionData")
  void testInvalidConversionsReturnUnset(final String value, final String typeName, final Class<?> expectedClass) {
    assertInvalidConversion(value, typeName, expectedClass);
  }

  @Test
  void testUnsupportedTypeThrowsException() {
    // Only unsupported type names should throw exceptions
    final var exception = assertThrows(TypeConversionException.class, () ->
        typeConverter.convertToEK9Type("test", "org.ek9.lang::UnsupportedType"));

    assertTrue(exception.getMessage().contains("Unsupported type conversion"));
    assertTrue(exception.getMessage().contains("org.ek9.lang::UnsupportedType"));
  }

  @Test
  void testNullUserValueThrowsException() {
    assertThrows(NullPointerException.class, () ->
        typeConverter.convertToEK9Type(null, "org.ek9.lang::String"));
  }

  @Test
  void testNullTypeNameThrowsException() {
    assertThrows(NullPointerException.class, () ->
        typeConverter.convertToEK9Type("test", null));
  }

  @Test
  void testGetSimpleTypeName() {
    assertEquals("String", typeConverter.getSimpleTypeName("org.ek9.lang::String"));
    assertEquals("Integer", typeConverter.getSimpleTypeName("org.ek9.lang::Integer"));
    assertEquals("Boolean", typeConverter.getSimpleTypeName("org.ek9.lang::Boolean"));
    assertEquals("Float", typeConverter.getSimpleTypeName("org.ek9.lang::Float"));

    // Test edge cases
    assertEquals("SimpleType", typeConverter.getSimpleTypeName("SimpleType"));
    assertEquals("Type", typeConverter.getSimpleTypeName("long::qualified::name::Type"));
  }

  @Test
  void testTypeConversionExceptionConstructors() {
    final var message = "Test message";
    final var cause = new RuntimeException("Cause");

    // Test both constructors
    final var exception1 = new TypeConversionException(message);
    assertEquals(message, exception1.getMessage());

    final var exception2 = new TypeConversionException(message, cause);
    assertEquals(message, exception2.getMessage());
    assertEquals(cause, exception2.getCause());
  }

  @Test
  void testEK9TriStateSemantics() {
    // Demonstrate EK9's tri-state model: present objects that can be set or unset

    // Valid conversion: present and set
    final var validInt = typeConverter.convertToEK9Type("42", "org.ek9.lang::Integer");
    assertNotNull(validInt);
    assertInstanceOf(org.ek9.lang.Integer.class, validInt);
    assertTrue(((org.ek9.lang.Integer) validInt)._isSet()._true());

    // Invalid conversion: present but unset (not null, not exception)
    final var invalidInt = typeConverter.convertToEK9Type("abc", "org.ek9.lang::Integer");
    assertNotNull(invalidInt);
    assertInstanceOf(org.ek9.lang.Integer.class, invalidInt);
    assertFalse(((org.ek9.lang.Integer) invalidInt)._isSet()._true());
  }

  // Test data providers

  static Stream<Arguments> validConversionData() {
    return Stream.of(
        // Integer conversions
        Arguments.of("42", "org.ek9.lang::Integer", org.ek9.lang.Integer.class),
        Arguments.of("0", "org.ek9.lang::Integer", org.ek9.lang.Integer.class),
        Arguments.of("-10", "org.ek9.lang::Integer", org.ek9.lang.Integer.class),
        Arguments.of("123456", "org.ek9.lang::Integer", org.ek9.lang.Integer.class),

        // Float conversions
        Arguments.of("3.14", "org.ek9.lang::Float", org.ek9.lang.Float.class),
        Arguments.of("2.0", "org.ek9.lang::Float", org.ek9.lang.Float.class),
        Arguments.of("-1.5", "org.ek9.lang::Float", org.ek9.lang.Float.class),
        Arguments.of("0.0", "org.ek9.lang::Float", org.ek9.lang.Float.class),
        Arguments.of("123.456", "org.ek9.lang::Float", org.ek9.lang.Float.class),

        // Character conversions
        Arguments.of("a", "org.ek9.lang::Character", org.ek9.lang.Character.class),
        Arguments.of("Z", "org.ek9.lang::Character", org.ek9.lang.Character.class),
        Arguments.of("1", "org.ek9.lang::Character", org.ek9.lang.Character.class),
        Arguments.of(" ", "org.ek9.lang::Character", org.ek9.lang.Character.class),

        // Bits conversions
        Arguments.of("0b01010101", "org.ek9.lang::Bits", org.ek9.lang.Bits.class),
        Arguments.of("0b11110000", "org.ek9.lang::Bits", org.ek9.lang.Bits.class),
        Arguments.of("0b1", "org.ek9.lang::Bits", org.ek9.lang.Bits.class),
        Arguments.of("0b0", "org.ek9.lang::Bits", org.ek9.lang.Bits.class),
        Arguments.of("0b", "org.ek9.lang::Bits", org.ek9.lang.Bits.class),

        // Date conversions
        Arguments.of("2024-04-05", "org.ek9.lang::Date", org.ek9.lang.Date.class),
        Arguments.of("2023-12-31", "org.ek9.lang::Date", org.ek9.lang.Date.class),
        Arguments.of("2025-01-01", "org.ek9.lang::Date", org.ek9.lang.Date.class),

        // DateTime conversions
        Arguments.of("2024-04-05T14:30:00Z", "org.ek9.lang::DateTime", org.ek9.lang.DateTime.class),
        Arguments.of("2023-12-31T23:59:59+01:00", "org.ek9.lang::DateTime", org.ek9.lang.DateTime.class),

        // Time conversions
        Arguments.of("14:30:00", "org.ek9.lang::Time", org.ek9.lang.Time.class),
        Arguments.of("09:15:30", "org.ek9.lang::Time", org.ek9.lang.Time.class),
        Arguments.of("00:00:00", "org.ek9.lang::Time", org.ek9.lang.Time.class),

        // Duration conversions
        Arguments.of("P1D", "org.ek9.lang::Duration", org.ek9.lang.Duration.class),
        Arguments.of("PT2H30M", "org.ek9.lang::Duration", org.ek9.lang.Duration.class),
        Arguments.of("P1DT2H30M", "org.ek9.lang::Duration", org.ek9.lang.Duration.class),

        // Millisecond conversions
        Arguments.of("100ms", "org.ek9.lang::Millisecond", org.ek9.lang.Millisecond.class),
        Arguments.of("250ms", "org.ek9.lang::Millisecond", org.ek9.lang.Millisecond.class),
        Arguments.of("1000ms", "org.ek9.lang::Millisecond", org.ek9.lang.Millisecond.class),

        // Dimension conversions
        Arguments.of("3.5em", "org.ek9.lang::Dimension", org.ek9.lang.Dimension.class),
        Arguments.of("10px", "org.ek9.lang::Dimension", org.ek9.lang.Dimension.class),
        Arguments.of("2.5rem", "org.ek9.lang::Dimension", org.ek9.lang.Dimension.class),

        // Resolution conversions
        Arguments.of("300dpi", "org.ek9.lang::Resolution", org.ek9.lang.Resolution.class),
        Arguments.of("800dpc", "org.ek9.lang::Resolution", org.ek9.lang.Resolution.class),

        // Colour conversions
        Arguments.of("#FF00FF", "org.ek9.lang::Colour", org.ek9.lang.Colour.class),
        Arguments.of("#000000", "org.ek9.lang::Colour", org.ek9.lang.Colour.class),
        Arguments.of("#FFFFFF", "org.ek9.lang::Colour", org.ek9.lang.Colour.class),

        // Money conversions (these may need adjustment based on actual EK9 Money format)
        Arguments.of("123.45#USD", "org.ek9.lang::Money", org.ek9.lang.Money.class),
        Arguments.of("50.00#EUR", "org.ek9.lang::Money", org.ek9.lang.Money.class),

        // RegEx conversions (discovered: forward slash delimited patterns)
        Arguments.of("/[a-zA-Z0-9]{6}/", "org.ek9.lang::RegEx", org.ek9.lang.RegEx.class),
        Arguments.of("/[S|s]te(?:ven?|phen)/", "org.ek9.lang::RegEx", org.ek9.lang.RegEx.class),
        Arguments.of("/.*\\/.*/", "org.ek9.lang::RegEx", org.ek9.lang.RegEx.class),

        // List<String> conversions (currently returns empty list - placeholder implementation)
        Arguments.of("item1", "org.ek9.lang::_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1",
            org.ek9.lang._List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1.class),
        Arguments.of("item1,item2,item3", "org.ek9.lang::_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1",
            org.ek9.lang._List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1.class),
        Arguments.of("", "org.ek9.lang::_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1",
            org.ek9.lang._List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1.class)
    );
  }

  static Stream<Arguments> invalidConversionData() {
    return Stream.of(
        // Invalid Integer conversions
        Arguments.of("abc", "org.ek9.lang::Integer", org.ek9.lang.Integer.class),
        Arguments.of("12.34", "org.ek9.lang::Integer", org.ek9.lang.Integer.class),
        Arguments.of("42abc", "org.ek9.lang::Integer", org.ek9.lang.Integer.class),
        Arguments.of("", "org.ek9.lang::Integer", org.ek9.lang.Integer.class),
        Arguments.of(" ", "org.ek9.lang::Integer", org.ek9.lang.Integer.class),

        // Invalid Float conversions
        Arguments.of("abc", "org.ek9.lang::Float", org.ek9.lang.Float.class),
        Arguments.of("not_a_float", "org.ek9.lang::Float", org.ek9.lang.Float.class),
        Arguments.of("", "org.ek9.lang::Float", org.ek9.lang.Float.class),
        Arguments.of(" ", "org.ek9.lang::Float", org.ek9.lang.Float.class),
        Arguments.of("3.14.15", "org.ek9.lang::Float", org.ek9.lang.Float.class),

        // Invalid Character conversions
        Arguments.of("", "org.ek9.lang::Character", org.ek9.lang.Character.class),
        Arguments.of("abc", "org.ek9.lang::Character", org.ek9.lang.Character.class),
        Arguments.of("toolong", "org.ek9.lang::Character", org.ek9.lang.Character.class),

        // Invalid Bits conversions
        Arguments.of("0b2", "org.ek9.lang::Bits", org.ek9.lang.Bits.class),
        Arguments.of("notbinary", "org.ek9.lang::Bits", org.ek9.lang.Bits.class),
        Arguments.of("", "org.ek9.lang::Bits", org.ek9.lang.Bits.class),

        // Invalid Date conversions
        Arguments.of("2024-13-01", "org.ek9.lang::Date", org.ek9.lang.Date.class),
        Arguments.of("2024-04-32", "org.ek9.lang::Date", org.ek9.lang.Date.class),
        Arguments.of("notdate", "org.ek9.lang::Date", org.ek9.lang.Date.class),
        Arguments.of("", "org.ek9.lang::Date", org.ek9.lang.Date.class),

        // Invalid DateTime conversions
        Arguments.of("2024-13-01T25:00:00", "org.ek9.lang::DateTime", org.ek9.lang.DateTime.class),
        Arguments.of("notdatetime", "org.ek9.lang::DateTime", org.ek9.lang.DateTime.class),
        Arguments.of("", "org.ek9.lang::DateTime", org.ek9.lang.DateTime.class),

        // Invalid Time conversions
        Arguments.of("25:00:00", "org.ek9.lang::Time", org.ek9.lang.Time.class),
        Arguments.of("14:60:00", "org.ek9.lang::Time", org.ek9.lang.Time.class),
        Arguments.of("nottime", "org.ek9.lang::Time", org.ek9.lang.Time.class),
        Arguments.of("", "org.ek9.lang::Time", org.ek9.lang.Time.class),

        // Invalid Duration conversions
        Arguments.of("invalidDuration", "org.ek9.lang::Duration", org.ek9.lang.Duration.class),
        Arguments.of("P", "org.ek9.lang::Duration", org.ek9.lang.Duration.class),
        Arguments.of("", "org.ek9.lang::Duration", org.ek9.lang.Duration.class),

        // Invalid Millisecond conversions
        Arguments.of("100", "org.ek9.lang::Millisecond", org.ek9.lang.Millisecond.class),
        Arguments.of("ms", "org.ek9.lang::Millisecond", org.ek9.lang.Millisecond.class),
        Arguments.of("notms", "org.ek9.lang::Millisecond", org.ek9.lang.Millisecond.class),
        Arguments.of("", "org.ek9.lang::Millisecond", org.ek9.lang.Millisecond.class),

        // Invalid Dimension conversions
        Arguments.of("3.5", "org.ek9.lang::Dimension", org.ek9.lang.Dimension.class),
        Arguments.of("em", "org.ek9.lang::Dimension", org.ek9.lang.Dimension.class),
        Arguments.of("notdimension", "org.ek9.lang::Dimension", org.ek9.lang.Dimension.class),
        Arguments.of("", "org.ek9.lang::Dimension", org.ek9.lang.Dimension.class),

        // Invalid Resolution conversions
        Arguments.of("1920", "org.ek9.lang::Resolution", org.ek9.lang.Resolution.class),
        Arguments.of("1920x", "org.ek9.lang::Resolution", org.ek9.lang.Resolution.class),
        Arguments.of("x1080", "org.ek9.lang::Resolution", org.ek9.lang.Resolution.class),
        Arguments.of("notresolution", "org.ek9.lang::Resolution", org.ek9.lang.Resolution.class),
        Arguments.of("", "org.ek9.lang::Resolution", org.ek9.lang.Resolution.class),

        // Invalid Colour conversions
        Arguments.of("#GG00FF", "org.ek9.lang::Colour", org.ek9.lang.Colour.class),
        Arguments.of("#FF", "org.ek9.lang::Colour", org.ek9.lang.Colour.class),
        Arguments.of("notcolor", "org.ek9.lang::Colour", org.ek9.lang.Colour.class),
        Arguments.of("", "org.ek9.lang::Colour", org.ek9.lang.Colour.class),

        // Invalid Money conversions
        Arguments.of("notmoney", "org.ek9.lang::Money", org.ek9.lang.Money.class),
        Arguments.of("123.45", "org.ek9.lang::Money", org.ek9.lang.Money.class),
        Arguments.of("USD", "org.ek9.lang::Money", org.ek9.lang.Money.class),
        Arguments.of("", "org.ek9.lang::Money", org.ek9.lang.Money.class),

        // Invalid RegEx conversions (patterns without forward slashes should be invalid)
        Arguments.of("//", "org.ek9.lang::RegEx", org.ek9.lang.RegEx.class),
        Arguments.of("pattern", "org.ek9.lang::RegEx", org.ek9.lang.RegEx.class),
        Arguments.of("[a-z]+", "org.ek9.lang::RegEx", org.ek9.lang.RegEx.class),
        Arguments.of("[", "org.ek9.lang::RegEx", org.ek9.lang.RegEx.class)
    );
  }

  // Helper methods

  /**
   * Assert that a conversion produces a valid (set) EK9 object.
   */
  private void assertValidConversion(final String value, final String typeName, final Class<?> expectedClass) {
    final var result = typeConverter.convertToEK9Type(value, typeName);
    assertNotNull(result);
    assertInstanceOf(expectedClass, result);
    assertTrue(((BuiltinType) result)._isSet()._true());
  }

  /**
   * Assert that a conversion produces an invalid (unset) EK9 object.
   */
  private void assertInvalidConversion(final String value, final String typeName, final Class<?> expectedClass) {
    final var result = typeConverter.convertToEK9Type(value, typeName);
    assertNotNull(result);
    assertInstanceOf(expectedClass, result);
    assertFalse(((BuiltinType) result)._isSet()._true());
  }

  @Test
  void testListStringConversionBehavior() {
    final var listTypeName = "org.ek9.lang::_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1";

    // Test that List<String> conversion always returns a set empty list (placeholder implementation)
    final var result1 = typeConverter.convertToEK9Type("item1", listTypeName);
    final var result2 = typeConverter.convertToEK9Type("item1,item2,item3", listTypeName);
    final var result3 = typeConverter.convertToEK9Type("", listTypeName);

    // All should return the expected type
    assertInstanceOf(org.ek9.lang._List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1.class, result1);
    assertInstanceOf(org.ek9.lang._List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1.class, result2);
    assertInstanceOf(org.ek9.lang._List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1.class, result3);

    // All should be set (collections are always set when created, even if empty)
    final var list1 = (org.ek9.lang._List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1) result1;
    final var list2 = (org.ek9.lang._List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1) result2;
    final var list3 = (org.ek9.lang._List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1) result3;

    assertTrue(list1._isSet()._true());
    assertTrue(list2._isSet()._true());
    assertTrue(list3._isSet()._true());

    // All should be empty (current placeholder implementation)
    final var expectedZero = org.ek9.lang.Integer._of(0);
    assertTrue(expectedZero._eq(list1._len())._true());
    assertTrue(expectedZero._eq(list2._len())._true());
    assertTrue(expectedZero._eq(list3._len())._true());
  }
}