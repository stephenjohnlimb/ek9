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

  public static final String ENTRY_LABEL = "entry";

  public static final String PARAM_SCOPE = "param";
  public static final String RETURN_SCOPE = "return";
  public static final String GENERAL_SCOPE = "scope";
  public static final String IF_SCOPE = "if";
  public static final String ELSE_SCOPE = "else";
  public static final String FOR_SCOPE = "for";
  public static final String DO_SCOPE = "do";
  public static final String WHILE_SCOPE = "while";

  public static final String C_INIT_METHOD = "c_init";
  public static final String I_INIT_METHOD = "i_init";
  public static final String TRUE_METHOD = "_true";
  public static final String FALSE_METHOD = "_false";

  public static final String VOID = "void";
  public static final String BOOLEAN = "boolean";


  public static final String RETURN_VARIABLE = "_rtn";
}
