package org.ek9lang.compiler.ir;

import java.util.List;

/**
 * Record representing a single condition case within a SWITCH_CHAIN_BLOCK.
 * <p>
 * Each condition case represents one evaluation path in the sequential chain:
 * - Question operator: NULL_CHECK case
 * - If statement: Boolean condition evaluation
 * - Switch case: Equality or expression evaluation
 * </p>
 * <p>
 * Contains complete evaluation instructions for both the condition check
 * and the corresponding body execution if the condition matches.
 * </p>
 */
public record ConditionCase(
    /**
     * Optional scope ID for case-level variable management.
     * Used when case bodies contain local variable declarations.
     */
    String caseScopeId,

    /**
     * Type of case condition for backend optimization hints.
     * - "NULL_CHECK": Null safety check (Question operator)
     * - "EXPRESSION": General boolean expression (if statements)
     * - "LITERAL": Direct value comparison (switch literals)
     * - "ENUM_CONSTANT": Enum constant comparison (switch enums)
     */
    String caseType,

    /**
     * For enum switch cases: the fully qualified enum constant name.
     * Example: "com.example.Color.RED"
     * Null for non-enum cases.
     */
    String enumConstant,

    /**
     * For enum switch cases: the ordinal value for jump table optimization.
     * -1 for non-enum cases.
     */
    int enumOrdinal,

    /**
     * Instructions to evaluate this condition.
     * For Question operator: IS_NULL check
     * For if statements: Boolean expression evaluation  
     * For switch: Equality or pattern matching evaluation
     */
    List<IRInstr> conditionEvaluation,

    /**
     * Variable containing the EK9 Boolean result of condition evaluation.
     * Used for EK9-level boolean operations and method calls.
     */
    String conditionResult,

    /**
     * Variable containing the primitive boolean result for backend optimization.
     * Backends use this for efficient branching and control flow.
     */
    String primitiveCondition,

    /**
     * Instructions to execute if this condition matches.
     * Contains the complete case body evaluation sequence.
     */
    List<IRInstr> bodyEvaluation,

    /**
     * Variable containing the result of body evaluation.
     * For expression forms: the computed result value
     * For statement forms: null or void indicator
     */
    String bodyResult
) {

  /**
   * Create a condition case for the Question operator null check.
   */
  public static ConditionCase createNullCheck(
      String caseScopeId,
      List<IRInstr> conditionEvaluation,
      String conditionResult,
      String primitiveCondition,
      List<IRInstr> bodyEvaluation,
      String bodyResult) {

    return new ConditionCase(
        caseScopeId,
        "NULL_CHECK",
        null, // No enum constant
        -1,   // No enum ordinal
        conditionEvaluation,
        conditionResult,
        primitiveCondition,
        bodyEvaluation,
        bodyResult
    );
  }

  /**
   * Create a condition case for general boolean expressions (if statements).
   */
  public static ConditionCase createExpression(
      String caseScopeId,
      List<IRInstr> conditionEvaluation,
      String conditionResult,
      String primitiveCondition,
      List<IRInstr> bodyEvaluation,
      String bodyResult) {

    return new ConditionCase(
        caseScopeId,
        "EXPRESSION",
        null, // No enum constant
        -1,   // No enum ordinal  
        conditionEvaluation,
        conditionResult,
        primitiveCondition,
        bodyEvaluation,
        bodyResult
    );
  }

  /**
   * Create a condition case for enum constant comparison (switch enum cases).
   */
  public static ConditionCase createEnumCase(
      String caseScopeId,
      String enumConstant,
      int enumOrdinal,
      List<IRInstr> conditionEvaluation,
      String conditionResult,
      String primitiveCondition,
      List<IRInstr> bodyEvaluation,
      String bodyResult) {

    return new ConditionCase(
        caseScopeId,
        "ENUM_CONSTANT",
        enumConstant,
        enumOrdinal,
        conditionEvaluation,
        conditionResult,
        primitiveCondition,
        bodyEvaluation,
        bodyResult
    );
  }

  /**
   * Create a condition case for literal value comparison (switch literal cases).
   */
  public static ConditionCase createLiteral(
      String caseScopeId,
      List<IRInstr> conditionEvaluation,
      String conditionResult,
      String primitiveCondition,
      List<IRInstr> bodyEvaluation,
      String bodyResult) {

    return new ConditionCase(
        caseScopeId,
        "LITERAL",
        null, // No enum constant
        -1,   // No enum ordinal
        conditionEvaluation,
        conditionResult,
        primitiveCondition,
        bodyEvaluation,
        bodyResult
    );
  }
}