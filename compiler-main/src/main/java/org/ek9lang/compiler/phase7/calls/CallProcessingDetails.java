package org.ek9lang.compiler.phase7.calls;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.phase7.support.VariableDetails;

/**
 * Details required for processing function calls in a context-aware manner.
 * Provides necessary information for both statement and expression contexts.
 */
public record CallProcessingDetails(
    EK9Parser.CallContext callContext,
    VariableDetails variableDetails,
    boolean isStatementContext
) {

  /**
   * Create processing details for statement context (full memory management).
   */
  public static CallProcessingDetails forStatement(EK9Parser.CallContext callContext, 
                                                  VariableDetails variableDetails) {
    return new CallProcessingDetails(callContext, variableDetails, true);
  }

  /**
   * Create processing details for expression context (minimal memory management).
   */
  public static CallProcessingDetails forExpression(EK9Parser.CallContext callContext,
                                                   VariableDetails variableDetails) {
    return new CallProcessingDetails(callContext, variableDetails, false);
  }


  /**
   * Get the result variable for this call.
   */
  public String resultVariable() {
    return variableDetails.resultVariable();
  }
}