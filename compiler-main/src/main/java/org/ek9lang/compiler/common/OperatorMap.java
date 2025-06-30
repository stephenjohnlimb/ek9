package org.ek9lang.compiler.common;

import org.ek9lang.core.CompilerException;

/**
 * Mapping of EK9 to java operators.
 */
public class OperatorMap extends BiMap {
  public OperatorMap() {
    put("<", "_lt");
    put("<=", "_lteq");
    put(">", "_gt");
    put(">=", "_gteq");
    put("==", "_eq");
    put("<>", "_neq");

    put("<<", "_shftl");
    put(">>", "_shftr");

    //Compare -1 0 +1
    put("<=>", "_cmp");
    //clone or copy all the details
    put(":=:", "_copy");
    //replace something in something i.e item in a List
    put(":^:", "_replace");

    //Fuzzy comparison
    put("<~>", "_fuzzy");
    //A merge which is sort of fuzzy! i.e you have to decide how to do it.
    put(":~:", "_merge");

    put("++", "_inc");
    put("--", "_dec");

    put("+", "_add");
    put("-", "_sub");
    put("*", "_mul");
    put("/", "_div");
    put("~", "_negate"); //can be used for boolean not and unary minus on objects compiler allows '-' with no args.
    put("!", "_fac");
    put("?", "_isSet"); //Used on objects to check if they are valid

    put(":=", "_assign");
    put("|", "_pipe");
    put("+=", "_addAss");
    put("-=", "_subAss");
    put("*=", "_mulAss");
    put("/=", "_divAss");

    put("and", "_and");
    put("or", "_or");
    put("xor", "_xor");

    put("#^", "_promote");
    put("$", "_string");
    put("$$", "_json");
    put("#?", "_hashCode");

    //Suitable for object types that can have prefix and suffix
    //EG for Money prefix is amount suffix is currency
    //But developers could add their own i.e telephone number might have a prefix (country dial code) for example.
    put("#<", "_prefix");
    put("#>", "_suffix");

    put("^", "_pow");
    put("mod", "_mod");
    put("rem", "_rem");

    put("close", "close");
    put("empty", "_empty");
    put("length", "_len");

    //So that reasonable method names can be used when they are also keywords for filtering we
    //add those in as operators - this also means that if you do create a collection in ek9 terms you call these methods
    put("sort", "_sort");
    put("filter", "_filter");
    put("collect", "_collect");
    put("map", "_map");
    put("group", "_group");
    put("split", "_split");
    put("head", "_head");
    put("tail", "_tail");

    put("sqrt", "_sqrt");
    put("abs", "_abs");
    put("contains", "_contains");
    put("matches", "_matches");
  }

  /**
   * For operators that require a single parameter.
   */
  public boolean expectsParameter(String ek9Operator) {
    return switch (ek9Operator) {
      case "<", "<=", ">", ">=", "==", "<>", "<=>", ":=:", ":^:", "<~>", ":~:", "+", "-", "*", "/", "|", "+=", "-=",
           "*=", "/=", "and", "or", "xor", "^", "mod", "rem", "contains", "matches" -> true;
      default -> false;
    };
  }

  /**
   * Used on a class/record not expecting any parameters at all.
   */
  public boolean expectsZeroParameters(String ek9Operator) {
    return switch (ek9Operator) {
      case "++", "--", "~", "!", "?", "$", "$$", "#^", "#?", "#<", "#>", "empty", "length", "sqrt", "abs" -> true;
      default -> false;
    };
  }

  /**
   * For a specific operator is the return type acceptable.
   * Only checks finite types we need like String, Boolean and Integer.
   */
  public boolean isReturnTypeNameAcceptable(String ek9Operator, String returnTypeName) {
    return switch (ek9Operator) {
      case "?", "<", "<=", ">", ">=", "<>", "empty", "contains", "matches" -> "Boolean".equals(returnTypeName);
      case "$" -> "String".equals(returnTypeName);
      case "$$" -> "JSON".equals(returnTypeName);
      case "#?", "<=>", "<~>", "length", "mod", "rem" -> "Integer".equals(returnTypeName);
      default -> true;
    };
  }

  @Override
  public String getForward(String v1) {
    String rtn = super.getForward(v1);
    if (rtn == null) {
      throw new CompilerException("Operator " + v1 + " does not exist");
    }
    return rtn;
  }

  @Override
  public String getBackward(String v2) {
    String rtn = super.getBackward(v2);
    if (rtn == null) {
      throw new CompilerException("Operator " + v2 + " does not exist");
    }
    return rtn;
  }

  public boolean checkForward(String v1) {
    return super.getForward(v1) != null;
  }

  /**
   * Check if a _ type method maps back into EK9 operators.
   * We might have methods we want in our java code that are just hidden.
   */
  public boolean checkBackward(String v2) {
    return super.getBackward(v2) != null;

  }
}
