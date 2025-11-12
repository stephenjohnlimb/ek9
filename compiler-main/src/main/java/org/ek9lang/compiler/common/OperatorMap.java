package org.ek9lang.compiler.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.ek9lang.core.CompilerException;

/**
 * Enhanced mapping of EK9 operators to their metadata and method names.
 * Provides bidirectional mapping with comprehensive operator details including
 * purity, argument requirements, return value information, and side effect classification.
 * <p>
 * Side effect types:<br>
 * - RETURN_MUTATION: Operations that return non-Void values<br>
 * - THIS_MUTATION: Operations that mutate the object itself (assignment/mutator operators)<br>
 * - POSSIBLE_MUTATION: Non-pure operations that may have indirect effects or mutations<br>
 * - NO_MUTATION: Implied when no mutation side effects are present (empty set)<br>
 * </p>
 */
public class OperatorMap {

  // Forward mapping: EK9 operator -> OperatorDetails
  private final Map<String, OperatorDetails> forwardMap = new HashMap<>();

  // Backward mapping: Method name -> OperatorDetails  
  private final Map<String, OperatorDetails> backwardMap = new HashMap<>();

  public OperatorMap() {
    // Comparison operators - pure, require argument, return Boolean
    addOperator("<", "_lt", true, true, true);
    addOperator("<=", "_lteq", true, true, true);
    addOperator(">", "_gt", true, true, true);
    addOperator(">=", "_gteq", true, true, true);
    addOperator("==", "_eq", true, true, true);
    addOperator("<>", "_neq", true, true, true);

    // Bit shift operators - pure, require argument, return same type
    addOperator("<<", "_shftl", true, true, true);
    addOperator(">>", "_shftr", true, true, true);

    // Special comparison and copy operators
    addOperator("<=>", "_cmp", true, true, true); // Compare -1 0 +1 - pure, returns Integer
    addOperator(":=:", "_copy", false, true, false); // Mutating copy - not pure, returns Void
    addOperator(":^:", "_replace", false, true, false); // Mutating replace - not pure, returns Void

    // Fuzzy operations
    addOperator("<~>", "_fuzzy", true, true, true); // Fuzzy comparison - pure, returns comparison result
    addOperator(":~:", "_merge", false, true, false); // Mutating merge - not pure, returns Void

    // Increment/decrement operators - mutating, no argument, return same type (not void)
    addOperator("++", "_inc", false, false, true);
    addOperator("--", "_dec", false, false, true);

    // Arithmetic operators - pure, require argument, return same/compatible type
    addOperator("+", "_add", true, true, true);
    addOperator("-", "_sub", true, true, true);
    addOperator("*", "_mul", true, true, true);
    addOperator("/", "_div", true, true, true);
    addOperator("~", "_negate", true, false, true); // Unary negate - pure, no argument, returns same type
    addOperator("!", "_fac", true, false, true); // Factorial - pure, no argument, returns result
    addOperator("?", "_isSet", true, false, true); // State check - pure, no argument, returns Boolean

    // Pipeline operator - mutating operation, requires argument, returns void
    addOperator("|", "_pipe", false, true, false);

    // Assignment operators - mutating, require argument, return void
    addOperator("+=", "_addAss", false, true, false);
    addOperator("-=", "_subAss", false, true, false);
    addOperator("*=", "_mulAss", false, true, false);
    addOperator("/=", "_divAss", false, true, false);

    // Logical operators - pure, require argument, return Boolean
    addOperator("and", "_and", true, true, true);
    addOperator("or", "_or", true, true, true);
    addOperator("xor", "_xor", true, true, true);

    // Conversion and introspection operators
    addOperator("#^", "_promote", true, false, true); // Type promotion - pure, no argument, returns promoted type
    addOperator("$", "_string", true, false, true); // String conversion - pure, no argument, returns String
    addOperator("$$", "_json", true, false, true); // JSON conversion - pure, no argument, returns String  
    addOperator("#?", "_hashcode", true, false, true); // Hash code - pure, no argument, returns Integer

    // Prefix/suffix operators - pure, no argument, return extracted value
    addOperator("#<", "_prefix", true, false, true);
    addOperator("#>", "_suffix", true, false, true);

    // Mathematical operators
    addOperator("^", "_pow", true, true, true); // Power - pure, requires argument, returns result
    addOperator("mod", "_mod", true, true,
        true); // Modulo - pure, requires argument, returns Integer per ValidOperatorOrError
    addOperator("rem", "_rem", true, true,
        true); // Remainder - pure, requires argument, returns Integer per ValidOperatorOrError

    // Resource and state operators
    addOperator("close", "_close", true, false,
        false); // Resource cleanup per ValidOperatorOrError - pure with no return
    addOperator("empty", "_empty", true, false, true); // State check - pure, no argument, returns Boolean
    addOperator("length", "_len", true, false, true); // Length query - pure, no argument, returns Integer

    // Collection operations - pure, no argument (for basic operations), return result
    addOperator("sort", "_sort", true, false, true);
    addOperator("filter", "_filter", true, false, true);
    addOperator("collect", "_collect", true, false, true);
    addOperator("map", "_map", true, false, true);
    addOperator("group", "_group", true, false, true);
    addOperator("split", "_split", true, false, true);
    addOperator("head", "_head", true, false, true);
    addOperator("tail", "_tail", true, false, true);

    // Mathematical functions - pure, no argument, return result
    addOperator("sqrt", "_sqrt", true, false, true);
    addOperator("abs", "_abs", true, false, true);

    // Query operations - pure, require argument, return Boolean
    addOperator("contains", "_contains", true, true, true);
    addOperator("matches", "_matches", true, true, true);
  }

  /**
   * Add an operator with its details to both forward and backward mappings.
   */
  private void addOperator(String operator, String methodName, boolean markedPure,
                           boolean requiresArgument, boolean hasReturn) {
    final var details = new OperatorDetails(operator, methodName, markedPure, requiresArgument, hasReturn);
    forwardMap.put(operator, details);
    backwardMap.put(methodName, details);
  }

  // Enhanced OperatorDetails-based methods

  /**
   * Get operator details by EK9 operator symbol.
   */
  public OperatorDetails getOperatorDetails(String ek9Operator) {
    final var details = forwardMap.get(ek9Operator);
    if (details == null) {
      throw new CompilerException("Operator " + ek9Operator + " does not exist");
    }
    return details;
  }

  /**
   * Get operator details by method name.
   */
  public OperatorDetails getOperatorDetailsByMethod(String methodName) {
    final var details = backwardMap.get(methodName);
    if (details == null) {
      throw new CompilerException("Method " + methodName + " does not map to an operator");
    }
    return details;
  }

  /**
   * Check if an EK9 operator exists.
   */
  public boolean hasOperator(String ek9Operator) {
    return forwardMap.containsKey(ek9Operator);
  }

  /**
   * Check if a method name maps to an operator.
   */
  public boolean hasMethod(String methodName) {
    return backwardMap.containsKey(methodName);
  }

  // Backward compatibility methods (delegate to OperatorDetails)

  /**
   * Get method name for EK9 operator (backward compatibility).
   */
  public String getForward(String ek9Operator) {
    return getOperatorDetails(ek9Operator).mappedName();
  }

  /**
   * Get EK9 operator for method name (backward compatibility).
   */
  public String getBackward(String methodName) {
    return getOperatorDetailsByMethod(methodName).operator();
  }

  /**
   * Check if EK9 operator exists (backward compatibility).
   */
  public boolean checkForward(String ek9Operator) {
    return hasOperator(ek9Operator);
  }

  /**
   * Check if method name maps to operator (backward compatibility).
   */
  public boolean checkBackward(String methodName) {
    return hasMethod(methodName);
  }

  /**
   * For operators that require a single parameter.
   * Updated to use OperatorDetails metadata.
   */
  public boolean expectsParameter(String ek9Operator) {
    final var details = forwardMap.get(ek9Operator);
    return details != null && details.requiresArgument();
  }

  /**
   * Used on a class/record not expecting any parameters at all.
   * Updated to use OperatorDetails metadata.
   */
  public boolean expectsZeroParameters(String ek9Operator) {
    final var details = forwardMap.get(ek9Operator);
    return details != null && !details.requiresArgument();
  }

  /**
   * Get the side effects for an EK9 operator.
   * Centralizes all side effect determination logic based on operator characteristics.
   */
  public Set<String> getSideEffects(String ek9Operator) {
    final var details = forwardMap.get(ek9Operator);
    if (details == null) {
      return new HashSet<>(); // Unknown operator, no known side effects
    }

    final var sideEffects = new HashSet<String>();

    // RETURN_MUTATION: operators that return non-Void values
    if (details.hasReturn()) {
      sideEffects.add("RETURN_MUTATION");
    }

    // THIS_MUTATION: operators that mutate the object itself
    if (isThisMutatingOperator(ek9Operator)) {
      sideEffects.add("THIS_MUTATION");
    }

    // POSSIBLE_MUTATION: non-pure operators that don't return values and aren't THIS_MUTATION
    // These may have indirect effects or mutations we can't precisely classify
    if (!details.markedPure() && !details.hasReturn() && !isThisMutatingOperator(ek9Operator)) {
      sideEffects.add("POSSIBLE_MUTATION");
    }

    return sideEffects;
  }

  /**
   * Get the side effects for a method name (backward lookup).
   * Convenience method for when you have the method name instead of the EK9 operator.
   */
  public Set<String> getSideEffectsByMethod(String methodName) {
    final var details = backwardMap.get(methodName);
    if (details == null) {
      return new HashSet<>(); // Unknown method, no known side effects
    }
    return getSideEffects(details.operator());
  }

  /**
   * Check if an operator mutates the object itself (THIS_MUTATION).
   * This includes assignment operators, increment/decrement, and mutator operations.
   */
  private boolean isThisMutatingOperator(String ek9Operator) {
    // Assignment operators: +=, -=, *=, /= (but NOT comparison operators like >=, <=, ==, <>)
    return (ek9Operator.endsWith("=")
        && !ek9Operator.equals(">=")
        && !ek9Operator.equals("<=")
        && !ek9Operator.equals("==")
        && !ek9Operator.equals("<>"))
        || ek9Operator.equals(":=:")
        || ek9Operator.equals(":^:")
        || ek9Operator.equals(":~:")
        || ek9Operator.equals("++")
        || ek9Operator.equals("--")
        || ek9Operator.equals("|");
  }

  // Methods for test compatibility

  /**
   * Get all EK9 operator symbols (for testing).
   */
  public Iterable<String> getForwardKeys() {
    return forwardMap.keySet();
  }

  /**
   * Get all method names (for testing).
   */
  public Iterable<String> getBackwardKeys() {
    return backwardMap.keySet();
  }
}
