package org.ek9lang.compiler.ir.data;

import java.util.List;
import javax.annotation.Nonnull;
import org.ek9lang.compiler.ir.instructions.IRInstr;

/**
 * Record containing default case management information for CONTROL_FLOW_CHAIN constructs.
 * <p>
 * Default cases handle the fallback behavior when no condition in the condition chain matches:
 * - if/else statements: "else" block
 * - switch statements: "default" case
 * - Question operators: the _isSet() check (default behavior when not null)
 * </p>
 * <p>
 * Some constructs may not have default cases (e.g., incomplete if statements without else,
 * guarded assignments that only execute conditionally).
 * </p>
 */
public record DefaultCaseDetails(
    /*
     * Instructions for the default/else case.
     * Executed when no condition in conditionChain matches.
     * Empty list if no default case exists.
     */
    List<IRInstr> defaultBodyEvaluation,

    /*
     * Result variable for the default case.
     * null if no default case or default produces no result.
     */
    String defaultResult
) {

  /**
   * Create empty default case details (no default case).
   */
  public static DefaultCaseDetails none() {
    return new DefaultCaseDetails(List.of(), null);
  }

  /**
   * Create default case details with instructions and result.
   */
  public static DefaultCaseDetails withResult(
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult) {
    return new DefaultCaseDetails(defaultBodyEvaluation, defaultResult);
  }

  /**
   * Check if this has a default case.
   */
  public boolean hasDefaultCase() {
    return defaultBodyEvaluation != null && !defaultBodyEvaluation.isEmpty();
  }

  /**
   * Check if this default details object is empty (no default case).
   */
  public boolean isEmpty() {
    return !hasDefaultCase();
  }

  /**
   * IR-optimized toString following EK9's bracket-only, no-indentation format.
   * Returns empty string if no default case to prevent rightward drift.
   */
  @Override
  @Nonnull
  public String toString() {
    if (isEmpty()) {
      return "";
    }

    var builder = new StringBuilder("[");
    builder.append("body=[");
    for (int i = 0; i < defaultBodyEvaluation.size(); i++) {
      if (i > 0) {
        builder.append(" ");
      }
      builder.append(defaultBodyEvaluation.get(i));
    }
    builder.append("]");

    if (defaultResult != null) {
      builder.append("result=").append(defaultResult);
    }

    return builder.append("]").toString();
  }
}