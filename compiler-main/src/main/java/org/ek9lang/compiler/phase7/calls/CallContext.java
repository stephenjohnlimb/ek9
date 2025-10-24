package org.ek9lang.compiler.phase7.calls;

import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
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
    String scopeId,
    ISymbol returnType,                      // Optional - return type from resolved method
    ParserRuleContext parseContext
) {

  /**
   * Create context for binary operation: left.method(right)
   */
  public static CallContext forBinaryOperation(ISymbol leftType, ISymbol rightType, ISymbol returnType,
                                               String methodName, String leftVariable, String rightVariable,
                                               String scopeId) {
    return new CallContext(leftType, leftVariable, methodName, List.of(rightType), List.of(rightVariable), scopeId,
        returnType, null);
  }

  /**
   * Create context for binary operation with parseContext: left.method(right)
   * Used when Phase 3 has already resolved the method (e.g., switch case comparisons)
   */
  public static CallContext forBinaryOperationWithContext(ISymbol leftType, ISymbol rightType, ISymbol returnType,
                                                          String methodName, String leftVariable, String rightVariable,
                                                          String scopeId, ParserRuleContext parseContext) {
    return new CallContext(leftType, leftVariable, methodName, List.of(rightType), List.of(rightVariable), scopeId,
        returnType, parseContext);
  }


  /**
   * Create context for unary operation: target.method()
   */
  public static CallContext forUnaryOperation(ISymbol targetType, String methodName,
                                              String targetVariable, ISymbol returnType, String scopeId) {
    return new CallContext(targetType, targetVariable, methodName, List.of(), List.of(), scopeId, returnType, null);
  }


  /**
   * Create context for function call: function(args...)
   */
  public static CallContext forFunctionCall(ISymbol targetType, List<ISymbol> argumentTypes, String methodName,
                                            String targetVariable, List<String> argumentVariables,
                                            String scopeId, EK9Parser.CallContext parseContext) {
    return new CallContext(targetType, targetVariable, methodName, argumentTypes, argumentVariables, scopeId,
        null, parseContext);
  }
}