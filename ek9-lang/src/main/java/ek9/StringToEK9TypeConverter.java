package ek9;

import static ek9.lang.EK9Types.EK9_BITS;
import static ek9.lang.EK9Types.EK9_BOOLEAN;
import static ek9.lang.EK9Types.EK9_CHARACTER;
import static ek9.lang.EK9Types.EK9_COLOUR;
import static ek9.lang.EK9Types.EK9_DATE;
import static ek9.lang.EK9Types.EK9_DATETIME;
import static ek9.lang.EK9Types.EK9_DIMENSION;
import static ek9.lang.EK9Types.EK9_DURATION;
import static ek9.lang.EK9Types.EK9_FLOAT;
import static ek9.lang.EK9Types.EK9_INTEGER;
import static ek9.lang.EK9Types.EK9_LIST_OF_STRING;
import static ek9.lang.EK9Types.EK9_MILLISECOND;
import static ek9.lang.EK9Types.EK9_MONEY;
import static ek9.lang.EK9Types.EK9_REGEX;
import static ek9.lang.EK9Types.EK9_RESOLUTION;
import static ek9.lang.EK9Types.EK9_STRING;
import static ek9.lang.EK9Types.EK9_TIME;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Pure conversion function that transforms string values to EK9 types.
 * Implements BiFunction&lt;String, String, Object&gt; for functional composition.
 * Follows EK9's tri-state semantics - returns unset objects for invalid conversions.
 */
public final class StringToEK9TypeConverter implements BiFunction<String, String, Object> {

  /**
   * Convert a string argument to the specified EK9 type.
   * Implements BiFunction&lt;String, String, Object&gt; interface.
   *
   * @param userValue   The string value from command line
   * @param ek9TypeName The fully qualified EK9 type name
   * @return The converted EK9 object
   * @throws TypeConverter.TypeConversionException if conversion fails or result is unset
   */
  @Override
  public Object apply(final String userValue, final String ek9TypeName) {
    Objects.requireNonNull(userValue, "User value cannot be null");
    Objects.requireNonNull(ek9TypeName, "EK9 type name cannot be null");

    return switch (ek9TypeName) {
      case EK9_STRING -> org.ek9.lang.String._of(userValue);
      case EK9_INTEGER -> org.ek9.lang.Integer._of(userValue);
      case EK9_BOOLEAN -> org.ek9.lang.Boolean._of(userValue);
      case EK9_FLOAT -> org.ek9.lang.Float._of(userValue);
      case EK9_CHARACTER -> org.ek9.lang.Character._of(userValue);
      case EK9_BITS -> org.ek9.lang.Bits._of(userValue);
      case EK9_DATE -> org.ek9.lang.Date._of(userValue);
      case EK9_DATETIME -> org.ek9.lang.DateTime._of(userValue);
      case EK9_TIME -> org.ek9.lang.Time._of(userValue);
      case EK9_DURATION -> org.ek9.lang.Duration._of(userValue);
      case EK9_MILLISECOND -> org.ek9.lang.Millisecond._of(userValue);
      case EK9_DIMENSION -> org.ek9.lang.Dimension._of(userValue);
      case EK9_RESOLUTION -> org.ek9.lang.Resolution._of(userValue);
      case EK9_COLOUR -> org.ek9.lang.Colour._of(userValue);
      case EK9_MONEY -> org.ek9.lang.Money._of(userValue);
      case EK9_REGEX -> org.ek9.lang.RegEx._of(userValue);
      case EK9_LIST_OF_STRING
          -> org.ek9.lang._List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
      default -> throw new TypeConverter.TypeConversionException("Unsupported type conversion: " + ek9TypeName);
    };
  }
}