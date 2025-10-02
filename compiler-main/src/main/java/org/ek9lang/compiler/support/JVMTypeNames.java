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

  // JVM type descriptors (L-wrapped)
  public static final String DESC_OBJECT = "Ljava/lang/Object;";
  public static final String DESC_PRINT_STREAM = "Ljava/io/PrintStream;";

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

  private JVMTypeNames() {
    //stop construction.
  }
}
