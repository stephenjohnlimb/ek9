package org.ek9lang.compiler.support;

/**
 * JVM internal type names and descriptors that the compiler uses when generating bytecode.
 */
public final class JVMTypeNames {
  // JVM internal type paths (slash-separated)
  public static final String JAVA_LANG_OBJECT = "java/lang/Object";
  public static final String JAVA_LANG_STRING = "java/lang/String";
  public static final String JAVA_LANG_INTEGER = "java/lang/Integer";
  public static final String JAVA_LANG_BOOLEAN = "java/lang/Boolean";
  public static final String JAVA_LANG_FLOAT = "java/lang/Float";
  public static final String JAVA_LANG_CHARACTER = "java/lang/Character";
  public static final String JAVA_LANG_STRING_BUILDER = "java/lang/StringBuilder";
  public static final String JAVA_LANG_SYSTEM = "java/lang/System";
  public static final String JAVA_LANG_EXCEPTION = "java/lang/Exception";
  public static final String JAVA_IO_PRINT_STREAM = "java/io/PrintStream";
  public static final String JAVA_UTIL_ARRAY_LIST = "java/util/ArrayList";
  public static final String JAVA_UTIL_LIST = "java/util/List";

  // EK9 built-in type internal names (slash-separated)
  public static final String EK9_LANG_ANY = "org/ek9/lang/Any";
  public static final String EK9_LANG_STRING = "org/ek9/lang/String";
  public static final String EK9_LANG_INTEGER = "org/ek9/lang/Integer";
  public static final String EK9_LANG_FLOAT = "org/ek9/lang/Float";
  public static final String EK9_LANG_BOOLEAN = "org/ek9/lang/Boolean";
  public static final String EK9_LANG_CHARACTER = "org/ek9/lang/Character";
  public static final String EK9_LANG_BITS = "org/ek9/lang/Bits";
  public static final String EK9_LANG_DATE = "org/ek9/lang/Date";
  public static final String EK9_LANG_DATETIME = "org/ek9/lang/DateTime";
  public static final String EK9_LANG_TIME = "org/ek9/lang/Time";
  public static final String EK9_LANG_DURATION = "org/ek9/lang/Duration";
  public static final String EK9_LANG_MILLISECOND = "org/ek9/lang/Millisecond";
  public static final String EK9_LANG_DIMENSION = "org/ek9/lang/Dimension";
  public static final String EK9_LANG_RESOLUTION = "org/ek9/lang/Resolution";
  public static final String EK9_LANG_COLOUR = "org/ek9/lang/Colour";
  public static final String EK9_LANG_MONEY = "org/ek9/lang/Money";
  public static final String EK9_LANG_REGEX = "org/ek9/lang/RegEx";
  public static final String EK9_LANG_BUILTIN_TYPE = "org/ek9/lang/BuiltinType";

  // JVM type descriptors (L-wrapped)
  public static final String DESC_OBJECT = "Ljava/lang/Object;";
  public static final String DESC_PRINT_STREAM = "Ljava/io/PrintStream;";

  // EK9 built-in type descriptors (L-wrapped)
  public static final String DESC_EK9_STRING = "Lorg/ek9/lang/String;";
  public static final String DESC_EK9_INTEGER = "Lorg/ek9/lang/Integer;";
  public static final String DESC_EK9_FLOAT = "Lorg/ek9/lang/Float;";
  public static final String DESC_EK9_BOOLEAN = "Lorg/ek9/lang/Boolean;";
  public static final String DESC_EK9_CHARACTER = "Lorg/ek9/lang/Character;";
  public static final String DESC_EK9_BITS = "Lorg/ek9/lang/Bits;";
  public static final String DESC_EK9_DATE = "Lorg/ek9/lang/Date;";
  public static final String DESC_EK9_DATETIME = "Lorg/ek9/lang/DateTime;";
  public static final String DESC_EK9_TIME = "Lorg/ek9/lang/Time;";
  public static final String DESC_EK9_DURATION = "Lorg/ek9/lang/Duration;";
  public static final String DESC_EK9_MILLISECOND = "Lorg/ek9/lang/Millisecond;";
  public static final String DESC_EK9_DIMENSION = "Lorg/ek9/lang/Dimension;";
  public static final String DESC_EK9_RESOLUTION = "Lorg/ek9/lang/Resolution;";
  public static final String DESC_EK9_COLOUR = "Lorg/ek9/lang/Colour;";
  public static final String DESC_EK9_MONEY = "Lorg/ek9/lang/Money;";
  public static final String DESC_EK9_REGEX = "Lorg/ek9/lang/RegEx;";

  // Common method descriptors
  public static final String DESC_STRING_TO_VOID = "(Ljava/lang/String;)V";
  public static final String DESC_VOID_TO_VOID = "()V";
  public static final String DESC_VOID_TO_STRING = "()Ljava/lang/String;";
  public static final String DESC_OBJECT_TO_BOOLEAN = "(Ljava/lang/Object;)Z";
  public static final String DESC_STRING_TO_STRING_BUILDER = "(Ljava/lang/String;)Ljava/lang/StringBuilder;";
  public static final String DESC_INT_TO_STRING_BUILDER = "(I)Ljava/lang/StringBuilder;";
  public static final String DESC_STRING_ARRAY_TO_VOID = "([Ljava/lang/String;)V";
  public static final String DESC_INT_TO_STRING = "(I)Ljava/lang/String;";
  public static final String DESC_BOOLEAN_TO_STRING = "(Z)Ljava/lang/String;";
  public static final String DESC_FLOAT_TO_STRING = "(F)Ljava/lang/String;";
  public static final String DESC_CHAR_TO_STRING = "(C)Ljava/lang/String;";

  // EK9 common method descriptors
  public static final String DESC_STRING_TO_EK9_STRING = "(Ljava/lang/String;)Lorg/ek9/lang/String;";
  public static final String DESC_STRING_TO_EK9_INTEGER = "(Ljava/lang/String;)Lorg/ek9/lang/Integer;";
  public static final String DESC_STRING_TO_EK9_FLOAT = "(Ljava/lang/String;)Lorg/ek9/lang/Float;";
  public static final String DESC_STRING_TO_EK9_BOOLEAN = "(Ljava/lang/String;)Lorg/ek9/lang/Boolean;";
  public static final String DESC_STRING_TO_EK9_CHARACTER = "(Ljava/lang/String;)Lorg/ek9/lang/Character;";
  public static final String DESC_STRING_TO_EK9_BITS = "(Ljava/lang/String;)Lorg/ek9/lang/Bits;";
  public static final String DESC_STRING_TO_EK9_DATE = "(Ljava/lang/String;)Lorg/ek9/lang/Date;";
  public static final String DESC_STRING_TO_EK9_DATETIME = "(Ljava/lang/String;)Lorg/ek9/lang/DateTime;";
  public static final String DESC_STRING_TO_EK9_TIME = "(Ljava/lang/String;)Lorg/ek9/lang/Time;";
  public static final String DESC_STRING_TO_EK9_DURATION = "(Ljava/lang/String;)Lorg/ek9/lang/Duration;";
  public static final String DESC_STRING_TO_EK9_MILLISECOND = "(Ljava/lang/String;)Lorg/ek9/lang/Millisecond;";
  public static final String DESC_STRING_TO_EK9_DIMENSION = "(Ljava/lang/String;)Lorg/ek9/lang/Dimension;";
  public static final String DESC_STRING_TO_EK9_RESOLUTION = "(Ljava/lang/String;)Lorg/ek9/lang/Resolution;";
  public static final String DESC_STRING_TO_EK9_COLOUR = "(Ljava/lang/String;)Lorg/ek9/lang/Colour;";
  public static final String DESC_STRING_TO_EK9_MONEY = "(Ljava/lang/String;)Lorg/ek9/lang/Money;";
  public static final String DESC_STRING_TO_EK9_REGEX = "(Ljava/lang/String;)Lorg/ek9/lang/RegEx;";
  public static final String DESC_VOID_TO_EK9_BOOLEAN = "()Lorg/ek9/lang/Boolean;";
  public static final String DESC_VOID_TO_BOOLEAN_PRIMITIVE = "()Z";

  // Descriptor parameter fragments for dynamic construction
  public static final String PARAM_STRING = "(Ljava/lang/String;)";
  public static final String PARAM_OBJECT = "(Ljava/lang/Object;)";
  public static final String PARAM_UTIL_LIST = "(Ljava/util/List;)";

  // JVM special method names
  public static final String METHOD_INIT = "<init>";
  public static final String METHOD_CLINIT = "<clinit>";

  // EK9 special method names (used in IR, converted to JVM equivalents)
  public static final String METHOD_I_INIT = "i_init";  // Instance field initializer
  public static final String METHOD_C_INIT = "c_init";  // Static initializer (IR name)
  public static final String METHOD_MAIN = "_main";     // EK9 program entry point

  // Function singleton pattern
  public static final String METHOD_GET_INSTANCE = "getInstance";  // Static getter for function singleton
  public static final String FIELD_INSTANCE = "INSTANCE";          // Static field for function singleton

  private JVMTypeNames() {
    //stop construction.
  }
}
