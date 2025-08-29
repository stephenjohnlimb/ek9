package org.ek9lang.compiler.support;

/**
 * Fixed and Known EK9 types that the compiler needs to know exist.
 */
public final class EK9TypeNames {
  public static final String EK9_LANG = "org.ek9.lang";
  //Special type parameterised list with String, so we can accept on the commandline.
  public static final String EK9_LIST_OF_STRING
      = EK9_LANG + "::_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1";
  public static final String EK9_COMPARATOR = EK9_LANG + "::Comparator";
  public static final String EK9_PREDICATE = EK9_LANG + "::Predicate";
  public static final String EK9_UNARY_OPERATOR = EK9_LANG + "::UnaryOperator";
  public static final String EK9_ROUTINE = EK9_LANG + "::Routine";
  public static final String EK9_FUNCTION = EK9_LANG + "::Function";
  public static final String EK9_CONSUMER = EK9_LANG + "::Consumer";
  public static final String EK9_SUPPLIER = EK9_LANG + "::Supplier";
  public static final String EK9_RESULT = EK9_LANG + "::Result";
  public static final String EK9_DICTIONARY_ENTRY = EK9_LANG + "::DictEntry";
  public static final String EK9_DICTIONARY = EK9_LANG + "::Dict";
  public static final String EK9_OPTIONAL = EK9_LANG + "::Optional";
  public static final String EK9_ITERATOR = EK9_LANG + "::Iterator";
  public static final String EK9_LIST = EK9_LANG + "::List";
  public static final String EK9_EXCEPTION = EK9_LANG + "::Exception";
  public static final String EK9_HTTP_RESPONSE = EK9_LANG + "::HTTPResponse";
  public static final String EK9_HTTP_REQUEST = EK9_LANG + "::HTTPRequest";
  public static final String EK9_JSON = EK9_LANG + "::JSON";
  public static final String EK9_BITS = EK9_LANG + "::Bits";
  public static final String EK9_BOOLEAN = EK9_LANG + "::Boolean";
  public static final String EK9_FLOAT = EK9_LANG + "::Float";
  public static final String EK9_VOID = EK9_LANG + "::Void";
  public static final String EK9_INTEGER = EK9_LANG + "::Integer";
  public static final String EK9_STRING = EK9_LANG + "::String";
  public static final String EK9_CHARACTER = EK9_LANG + "::Character";
  public static final String EK9_TIME = EK9_LANG + "::Time";
  public static final String EK9_DATE = EK9_LANG + "::Date";
  public static final String EK9_DATETIME = EK9_LANG + "::DateTime";
  public static final String EK9_DURATION = EK9_LANG + "::Duration";
  public static final String EK9_MILLISECOND = EK9_LANG + "::Millisecond";
  public static final String EK9_DIMENSION = EK9_LANG + "::Dimension";
  public static final String EK9_RESOLUTION = EK9_LANG + "::Resolution";
  public static final String EK9_COLOUR = EK9_LANG + "::Colour";
  public static final String EK9_MONEY = EK9_LANG + "::Money";
  public static final String EK9_REGEX = EK9_LANG + "::RegEx";
  public static final String EK9_VERSION = EK9_LANG + "::Version";
  public static final String EK9_PATH = EK9_LANG + "::Path";
  public static final String EK9_ANY = EK9_LANG + "::Any";
  public static final String EK9_MATH = "org.ek9.math";

  private EK9TypeNames() {
    //stop construction.
  }
}
