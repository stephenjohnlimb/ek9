package org.ek9lang.compiler.phase7.support;

/**
 * Rather than smatter the Java code with 'stringy' constants for the IR, they are defined here.
 * Basically we need to accept that there are some 'types' and specific method names that the
 * backend will need to support.
 * <p>
 * For example, we will need to 'expect' that the concept of a 'boolean' exists and when working
 * with ASM(JVM) this will be a Java boolean, When working with LLVM-C++ it will be a 'bool'/i8.
 * </p>
 * <p>
 * But in the EK9 IR we will need to refer to these 'primitive' types. For example, when branching, or
 * loading int main(int argc, char *argv[]).
 * </p>
 */
public final class IRConstants {

  private IRConstants() {
    //Just to stop creation.
  }

  //Known object references
  public static final String THIS = "this";
  public static final String SUPER = "super";

  //labels
  public static final String ENTRY_LABEL = "entry";

  //scopes
  public static final String PARAM_SCOPE = "param";
  public static final String RETURN_SCOPE = "return";
  public static final String GENERAL_SCOPE = "scope";
  public static final String IF_SCOPE = "if";
  public static final String ELSE_SCOPE = "else";
  public static final String FOR_SCOPE = "for";
  public static final String DO_SCOPE = "do";
  public static final String WHILE_SCOPE = "while";

  //method calls
  public static final String INIT_METHOD = "<init>";
  public static final String CALL_METHOD = "_call";
  public static final String C_INIT_METHOD = "c_init";
  public static final String I_INIT_METHOD = "i_init";
  public static final String TRUE_METHOD = "_true";
  public static final String FALSE_METHOD = "_false";

  //primitive types
  public static final String VOID = "void";
  public static final String BOOLEAN = "boolean";

  //known specific variable names
  public static final String RETURN_VARIABLE = "_rtn";

  //known temporary variable prefix
  public static final String TEMP_C_INIT = "_temp_c_init";
  public static final String TEMP_I_INIT = "_temp_i_init";
  public static final String TEMP_SUPER_INIT = "_temp_super_init";

  //access_modifiers
  public static final String PUBLIC = "public";
  public static final String PRIVATE = "private";

}
