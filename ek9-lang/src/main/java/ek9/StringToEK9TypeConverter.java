package ek9;

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
      case "org.ek9.lang::String" -> org.ek9.lang.String._of(userValue);
      case "org.ek9.lang::Integer" -> org.ek9.lang.Integer._of(userValue);
      case "org.ek9.lang::Boolean" -> org.ek9.lang.Boolean._of(userValue);
      case "org.ek9.lang::Float" -> org.ek9.lang.Float._of(userValue);
      case "org.ek9.lang::Character" -> org.ek9.lang.Character._of(userValue);
      case "org.ek9.lang::Bits" -> org.ek9.lang.Bits._of(userValue);
      case "org.ek9.lang::Date" -> org.ek9.lang.Date._of(userValue);
      case "org.ek9.lang::DateTime" -> org.ek9.lang.DateTime._of(userValue);
      case "org.ek9.lang::Time" -> org.ek9.lang.Time._of(userValue);
      case "org.ek9.lang::Duration" -> org.ek9.lang.Duration._of(userValue);
      case "org.ek9.lang::Millisecond" -> org.ek9.lang.Millisecond._of(userValue);
      case "org.ek9.lang::Dimension" -> org.ek9.lang.Dimension._of(userValue);
      case "org.ek9.lang::Resolution" -> org.ek9.lang.Resolution._of(userValue);
      case "org.ek9.lang::Colour" -> org.ek9.lang.Colour._of(userValue);
      case "org.ek9.lang::Money" -> org.ek9.lang.Money._of(userValue);
      case "org.ek9.lang::RegEx" -> org.ek9.lang.RegEx._of(userValue);
      case "org.ek9.lang::_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1"
          -> org.ek9.lang._List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of();
      default -> throw new TypeConverter.TypeConversionException("Unsupported type conversion: " + ek9TypeName);
    };
  }
}