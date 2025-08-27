package org.ek9lang.compiler.ir;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * Record containing evaluation variable management information for CONTROL_FLOW_CHAIN constructs.
 * <p>
 * Evaluation variables are used in switch statements where the switch expression
 * is computed and stored in a variable for comparison against case conditions:
 * - switch statements: "switch someVar" or "switch var := expr"
 * - switch expressions: "result &lt;- switch someVar"
 * </p>
 * <p>
 * For if/else statements and Question operators, evaluation variables are not used
 * since conditions are evaluated directly as boolean expressions.
 * </p>
 */
public record EvaluationVariableDetails(

    /*
     * Variable being evaluated (for switch statements).
     * null for if/else statements and Question operators.
     * Example: the variable in "switch someVar"
     */
    String evaluationVariable,

    /*
     * Fully qualified type name of the evaluation variable.
     * null if evaluationVariable is null.
     */
    String evaluationVariableType,

    /*
     * Instructions to setup/compute the evaluation variable.
     * Used for switch declarations like "switch var := expr"
     * Empty list if no setup is needed.
     */
    List<IRInstr> evaluationVariableSetup
) {

  /**
   * Create empty evaluation variable details (no evaluation variable).
   */
  public static EvaluationVariableDetails none() {
    return new EvaluationVariableDetails(null, null, List.of());
  }

  /**
   * Create evaluation variable details for existing variable.
   */
  public static EvaluationVariableDetails forVariable(
      String evaluationVariable,
      String evaluationVariableType) {
    return new EvaluationVariableDetails(evaluationVariable, evaluationVariableType, List.of());
  }

  /**
   * Create evaluation variable details with setup instructions.
   */
  public static EvaluationVariableDetails withSetup(
      String evaluationVariable,
      String evaluationVariableType,
      List<IRInstr> evaluationVariableSetup) {
    return new EvaluationVariableDetails(evaluationVariable, evaluationVariableType, evaluationVariableSetup);
  }

  /**
   * Check if this has an evaluation variable.
   */
  public boolean hasEvaluationVariable() {
    return evaluationVariable != null;
  }

  /**
   * Check if this has setup instructions.
   */
  public boolean hasSetupInstructions() {
    return evaluationVariableSetup != null && !evaluationVariableSetup.isEmpty();
  }

  /**
   * Check if this evaluation details object is empty (no evaluation variable).
   */
  public boolean isEmpty() {
    return !hasEvaluationVariable();
  }

  /**
   * IR-optimized toString following EK9's bracket-only, no-indentation format.
   * Returns empty string if no evaluation variable to prevent rightward drift.
   */
  @Override
  @Nonnull
  public String toString() {
    if (isEmpty()) {
      return "";
    }

    var builder = new StringBuilder("[");
    builder.append("var=").append(evaluationVariable);

    if (evaluationVariableType != null) {
      builder.append(", type=").append(evaluationVariableType);
    }

    if (hasSetupInstructions()) {
      builder.append(", setup=[");
      for (int i = 0; i < evaluationVariableSetup.size(); i++) {
        if (i > 0) {
          builder.append(" ");
        }
        builder.append(evaluationVariableSetup.get(i));
      }
      builder.append("]");
    }

    return builder.append("]").toString();
  }
}