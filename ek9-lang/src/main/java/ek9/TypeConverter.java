package ek9;

/**
 * Coordinates string-to-EK9-type conversion and provides utility methods.
 * Uses StringToEK9TypeConverter for the actual conversion logic.
 * Provides error message formatting and type name simplification utilities.
 */
public final class TypeConverter {

  private final StringToEK9TypeConverter converter = new StringToEK9TypeConverter();

  /**
   * Convert a string argument to the specified EK9 type.
   * Delegates to StringToEK9TypeConverter for the actual conversion.
   *
   * @param userValue   The string value from command line
   * @param ek9TypeName The fully qualified EK9 type name
   * @return The converted EK9 object
   * @throws TypeConversionException if conversion fails or result is unset
   */
  public Object convertToEK9Type(final String userValue, final String ek9TypeName) {
    return converter.apply(userValue, ek9TypeName);
  }

  /**
   * Get a user-friendly type name for error messages.
   * "org.ek9.lang::String" → "String"
   */
  public String getSimpleTypeName(final String ek9TypeName) {
    final var lastColon = ek9TypeName.lastIndexOf("::");
    return lastColon >= 0 ? ek9TypeName.substring(lastColon + 2) : ek9TypeName;
  }

  /**
   * Exception thrown when type conversion fails.
   */
  public static class TypeConversionException extends RuntimeException {
    public TypeConversionException(final String message) {
      super(message);
    }

    public TypeConversionException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}