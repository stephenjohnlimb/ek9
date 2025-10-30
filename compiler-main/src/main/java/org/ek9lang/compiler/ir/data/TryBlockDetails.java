package org.ek9lang.compiler.ir.data;

import java.util.List;
import javax.annotation.Nonnull;
import org.ek9lang.compiler.ir.instructions.IRInstr;

/**
 * Record representing the try block within a TRY_CATCH_FINALLY control flow chain.
 * <p>
 * Contains the try block scope and body evaluation instructions. Resources (if any)
 * are managed through the guard variable mechanism in ControlFlowChainDetails.
 * </p>
 * <p>
 * The try block represents the primary execution path that may throw exceptions.
 * If an exception is thrown, control transfers to the appropriate catch handler
 * (if any), followed by the finally block (if present).
 * </p>
 */
public record TryBlockDetails(
    /*
     * Scope ID for try block execution.
     * Variables declared in the try block are isolated to prevent
     * sharing with catch/finally blocks (except through explicit returns).
     */
    String tryScopeId,

    /*
     * Instructions to execute in the try block body.
     * Contains the complete evaluation sequence that may throw exceptions.
     */
    List<IRInstr> tryBodyEvaluation,

    /*
     * Variable containing the result of try block evaluation.
     * For expression forms: the computed result value
     * For statement forms: null or void indicator
     * Used to store the try block result before control transfers.
     */
    String tryBodyResult
) {

  /**
   * IR-optimized toString following EK9's bracket-only, no-indentation format.
   */
  @Override
  @Nonnull
  public String toString() {
    var builder = new StringBuilder("[");

    if (tryScopeId != null) {
      builder.append("try_scope_id: ").append(tryScopeId);
    }

    builder.append("try_body_evaluation: [");
    for (int i = 0; i < tryBodyEvaluation.size(); i++) {
      if (i > 0) {
        builder.append(" ");
      }
      builder.append(tryBodyEvaluation.get(i));
    }
    builder.append("]");

    if (tryBodyResult != null) {
      builder.append("try_body_result: ").append(tryBodyResult);
    }

    return builder.append("]").toString();
  }
}
