package org.ek9lang.compiler.ir.data;

import java.util.List;
import javax.annotation.Nonnull;
import org.ek9lang.compiler.ir.instructions.IRInstr;

/**
 * Record representing a single condition case within a CONTROL_FLOW_CHAIN.
 * <p>
 * Each condition case represents one evaluation path in the sequential chain:
 * - Question operator: NULL_CHECK case
 * - If statement: Boolean condition evaluation
 * - Switch case: Equality or expression evaluation
 * - Guard conditions: Combined assignment + condition evaluation
 * - Exception handlers: Type-based exception matching (try/catch)
 * </p>
 * <p>
 * Contains complete evaluation instructions for guard updates, condition checks,
 * and corresponding body execution if the condition matches.
 * </p>
 */
public record ConditionCaseDetails(
    /*
     * Optional scope ID for case-level variable management.
     * Used when case bodies contain local variable declarations.
     */
    String caseScopeId,

    /*
     * Type of case condition for backend optimization hints.
     * - "NULL_CHECK": Null safety check (Question operator)
     * - "EXPRESSION": General boolean expression (if statements)
     * - "GUARD_CONDITION": Guard variable assignment + condition (if/switch with guards)
     * - "LITERAL": Direct value comparison (switch literals)
     * - "ENUM_CONSTANT": Enum constant comparison (switch enums)
     * - "EXCEPTION_HANDLER": Exception type matching (try/catch blocks)
     */
    String caseType,

    /*
     * Instructions to update guard variables before condition evaluation.
     * Used for subsequent cases that need to reassign guard variables.
     * Empty list if no guard updates are needed (first case or no guards).
     */
    List<IRInstr> guardUpdates,

    /*
     * Scope ID for body execution isolation.
     * Each body gets its own scope to prevent variable sharing across mutually exclusive paths.
     * null if body should use parent scope.
     */
    String bodyScopeId,

    /*
     * For enum switch cases: the fully qualified enum constant name.
     * Example: "com.example.Color.RED"
     * Null for non-enum cases.
     */
    String enumConstant,

    /*
     * For enum switch cases: the ordinal value for jump table optimization.
     * -1 for non-enum cases.
     */
    int enumOrdinal,

    /*
     * Instructions to evaluate this condition.
     * For Question operator: IS_NULL check
     * For if statements: Boolean expression evaluation
     * For switch: Equality or pattern matching evaluation
     */
    List<IRInstr> conditionEvaluation,

    /*
     * Variable containing the EK9 Boolean result of condition evaluation.
     * Used for EK9-level boolean operations and method calls.
     */
    String conditionResult,

    /*
     * Variable containing the primitive boolean result for backend optimization.
     * Backends use this for efficient branching and control flow.
     */
    String primitiveCondition,

    /*
     * Instructions to execute if this condition matches.
     * Contains the complete case body evaluation sequence.
     */
    List<IRInstr> bodyEvaluation,

    /*
     * Variable containing the result of body evaluation.
     * For expression forms: the computed result value
     * For statement forms: null or void indicator
     */
    String bodyResult,

    /*
     * For exception handler cases: the fully qualified exception type.
     * Example: "org.ek9.lang::Exception" or "org.ek9.lang::IOException"
     * Null for non-exception cases.
     */
    String exceptionType,

    /*
     * For exception handler cases: the exception variable name.
     * Example: "ex" or "ioEx"
     * Null for non-exception cases.
     */
    String exceptionVariable
) {

  /**
   * Create a condition case for the Question operator null check.
   */
  public static ConditionCaseDetails createNullCheck(
      String caseScopeId,
      List<IRInstr> conditionEvaluation,
      String conditionResult,
      String primitiveCondition,
      List<IRInstr> bodyEvaluation,
      String bodyResult) {

    return new ConditionCaseDetails(
        caseScopeId,
        "NULL_CHECK",
        List.of(), // No guard updates
        null, // No body scope ID (question operator is self-contained)
        null, // No enum constant
        -1,   // No enum ordinal
        conditionEvaluation,
        conditionResult,
        primitiveCondition,
        bodyEvaluation,
        bodyResult,
        null, // No exception type
        null  // No exception variable
    );
  }

  /**
   * Create a condition case for general boolean expressions (if statements).
   */
  public static ConditionCaseDetails createExpression(
      String caseScopeId,
      List<IRInstr> conditionEvaluation,
      String conditionResult,
      String primitiveCondition,
      List<IRInstr> bodyEvaluation,
      String bodyResult) {

    return new ConditionCaseDetails(
        caseScopeId,
        "EXPRESSION",
        List.of(), // No guard updates
        null, // No body scope ID
        null, // No enum constant
        -1,   // No enum ordinal
        conditionEvaluation,
        conditionResult,
        primitiveCondition,
        bodyEvaluation,
        bodyResult,
        null, // No exception type
        null  // No exception variable
    );
  }

  /**
   * Create a condition case for literal value comparison (switch literal cases).
   */
  public static ConditionCaseDetails createLiteral(
      String caseScopeId,
      List<IRInstr> conditionEvaluation,
      String conditionResult,
      String primitiveCondition,
      List<IRInstr> bodyEvaluation,
      String bodyResult) {

    return new ConditionCaseDetails(
        caseScopeId,
        "LITERAL",
        List.of(), // No guard updates
        null, // No body scope ID
        null, // No enum constant
        -1,   // No enum ordinal
        conditionEvaluation,
        conditionResult,
        primitiveCondition,
        bodyEvaluation,
        bodyResult,
        null, // No exception type
        null  // No exception variable
    );
  }

  /**
   * Create a condition case for exception handlers (try/catch blocks).
   * Exception handlers match by type rather than Boolean condition evaluation.
   */
  public static ConditionCaseDetails createExceptionHandler(
      String catchScopeId,
      String exceptionType,
      String exceptionVariable,
      String catchBodyScopeId,
      List<IRInstr> catchBodyEvaluation,
      String catchBodyResult) {

    return new ConditionCaseDetails(
        catchScopeId,
        "EXCEPTION_HANDLER",
        List.of(), // No guard updates
        catchBodyScopeId,
        null, // No enum constant
        -1,   // No enum ordinal
        List.of(), // No condition evaluation (type matching is implicit)
        null, // No condition result (type match, not Boolean)
        null, // No primitive condition
        catchBodyEvaluation,
        catchBodyResult,
        exceptionType,
        exceptionVariable
    );
  }

  /**
   * IR-optimized toString following EK9's bracket-only, no-indentation format.
   */
  @Override
  @Nonnull
  public String toString() {
    var builder = new StringBuilder("[");

    if (caseScopeId != null) {
      builder.append("case_scope_id: ").append(caseScopeId);
    }

    builder.append("case_type: \"").append(caseType).append("\"");

    if (!guardUpdates.isEmpty()) {
      builder.append("guard_updates: [");
      for (int i = 0; i < guardUpdates.size(); i++) {
        if (i > 0) {
          builder.append(" ");
        }
        builder.append(guardUpdates.get(i));
      }
      builder.append("]");
    }

    if (bodyScopeId != null) {
      builder.append("body_scope_id: ").append(bodyScopeId);
    }

    if (enumConstant != null) {
      builder.append("enum_constant: \"").append(enumConstant).append("\"");
      builder.append("enum_ordinal: ").append(enumOrdinal);
    }

    builder.append("condition_evaluation: [");
    for (int i = 0; i < conditionEvaluation.size(); i++) {
      if (i > 0) {
        builder.append(" ");
      }
      builder.append(conditionEvaluation.get(i));
    }
    builder.append("]");

    if (conditionResult != null) {
      builder.append("condition_result: ").append(conditionResult);
    }

    if (primitiveCondition != null) {
      builder.append("primitive_condition: ").append(primitiveCondition);
    }

    builder.append("body_evaluation: [");
    for (int i = 0; i < bodyEvaluation.size(); i++) {
      if (i > 0) {
        builder.append(" ");
      }
      builder.append(bodyEvaluation.get(i));
    }
    builder.append("]");

    if (bodyResult != null) {
      builder.append("body_result: ").append(bodyResult);
    }

    if (exceptionType != null) {
      builder.append("exception_type: \"").append(exceptionType).append("\"");
    }

    if (exceptionVariable != null) {
      builder.append("exception_variable: ").append(exceptionVariable);
    }

    return builder.append("]").toString();
  }
}