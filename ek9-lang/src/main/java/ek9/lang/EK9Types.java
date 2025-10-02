package ek9.lang;

/**
 * EK9 type name constants for the standard library runtime.
 * These constants mirror those in compiler-main's EK9TypeNames but are needed
 * here since ek9-lang cannot depend on compiler-main (circular dependency).
 */
public final class EK9Types {
  public static final String EK9_LANG = "org.ek9.lang";

  // Basic types
  public static final String EK9_STRING = EK9_LANG + "::String";
  public static final String EK9_INTEGER = EK9_LANG + "::Integer";
  public static final String EK9_BOOLEAN = EK9_LANG + "::Boolean";
  public static final String EK9_FLOAT = EK9_LANG + "::Float";
  public static final String EK9_CHARACTER = EK9_LANG + "::Character";
  public static final String EK9_BITS = EK9_LANG + "::Bits";

  // Date/Time types
  public static final String EK9_DATE = EK9_LANG + "::Date";
  public static final String EK9_DATETIME = EK9_LANG + "::DateTime";
  public static final String EK9_TIME = EK9_LANG + "::Time";
  public static final String EK9_DURATION = EK9_LANG + "::Duration";
  public static final String EK9_MILLISECOND = EK9_LANG + "::Millisecond";

  // Physical/Visual types
  public static final String EK9_DIMENSION = EK9_LANG + "::Dimension";
  public static final String EK9_RESOLUTION = EK9_LANG + "::Resolution";
  public static final String EK9_COLOUR = EK9_LANG + "::Colour";

  // Financial/Pattern types
  public static final String EK9_MONEY = EK9_LANG + "::Money";
  public static final String EK9_REGEX = EK9_LANG + "::RegEx";

  // Special parameterized list with String
  public static final String EK9_LIST_OF_STRING
      = EK9_LANG + "::_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1";

  private EK9Types() {
    // Prevent instantiation
  }
}
