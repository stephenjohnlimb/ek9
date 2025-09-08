package org.ek9lang.compiler.phase7.support;

import java.util.List;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Context information needed for resolving method calls and constructing CallDetails.
 * Contains all the information needed to perform cost-based method resolution
 * and parameter-by-parameter promotion checking.
 */
public record CallContext(
    ISymbol targetType,
    String targetVariable,
    String methodName,
    List<ISymbol> argumentTypes,
    List<String> argumentVariables,
    String scopeId
) {

  /**
   * Create context for binary operation: left.method(right)
   */
  public static CallContext forBinaryOperation(ISymbol leftType, ISymbol rightType,
                                               String methodName, String leftVariable, String rightVariable,
                                               String scopeId) {
    return new CallContext(leftType, leftVariable, methodName, List.of(rightType), List.of(rightVariable), scopeId);
  }

  /**
   * Create context for unary operation: target.method()
   */
  public static CallContext forUnaryOperation(ISymbol targetType, String methodName,
                                             String targetVariable, String scopeId) {
    return new CallContext(targetType, targetVariable, methodName, List.of(), List.of(), scopeId);
  }

  /**
   * Create context for method call: target.method(args...)
   */
  public static CallContext forMethodCall(ISymbol targetType, String targetVariable, String methodName,
                                         List<ISymbol> argumentTypes, List<String> argumentVariables,
                                         String scopeId) {
    return new CallContext(targetType, targetVariable, methodName, argumentTypes, argumentVariables, scopeId);
  }
}