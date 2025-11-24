package org.ek9lang.compiler.ir.data;

import java.util.List;
import javax.annotation.Nonnull;
import org.ek9lang.compiler.ir.instructions.IRInstr;

/**
 * Record containing return variable management information for CONTROL_FLOW_CHAIN constructs.
 * <p>
 * Return variables are used in expression forms of control flow constructs where
 * the result of the construct is assigned to a declared variable:
 * - switch expressions: "result &lt;- switch someVar"
 * - if expressions: "result &lt;- if condition then value1 else value2"
 * </p>
 * <p>
 * For statement forms (if statements, switch statements), return variables are not used
 * since the construct doesn't produce a result value.
 * </p>
 */
public record ReturnVariableDetails(
    /*
     * Explicit return variable for expression forms.
     * Used when switch/if is an expression with declared return variable.
     * Example: the "result" in "result <- switch expr"
     * null for statement forms.
     */
    String returnVariable,

    /*
     * Type of the return variable.
     * null if returnVariable is null.
     */
    String returnVariableType,

    /*
     * Instructions to setup the return variable.
     * Typically REFERENCE declaration and scope registration.
     * Empty list if no setup is needed.
     */
    List<IRInstr> returnVariableSetup
) {

  /**
   * Create empty return variable details (no return variable).
   */
  public static ReturnVariableDetails none() {
    return new ReturnVariableDetails(null, null, List.of());
  }

  /**
   * Create return variable details with setup instructions.
   */
  public static ReturnVariableDetails withSetup(
      String returnVariable,
      String returnVariableType,
      List<IRInstr> returnVariableSetup) {
    return new ReturnVariableDetails(returnVariable, returnVariableType, returnVariableSetup);
  }

  /**
   * Check if this has a return variable.
   */
  public boolean hasReturnVariable() {
    return returnVariable != null;
  }

  /**
   * Check if this return details object is empty (no return variable).
   */
  public boolean isEmpty() {
    return !hasReturnVariable();
  }

  /**
   * IR-optimized toString following EK9's bracket-only, no-indentation format.
   * Returns empty string if no return variable to prevent rightward drift.
   */
  @Override
  @Nonnull
  public String toString() {
    if (isEmpty()) {
      return "";
    }

    var builder = new StringBuilder("[");
    builder.append("var=").append(returnVariable);

    if (returnVariableType != null) {
      builder.append(", type=").append(returnVariableType);
    }

    if (returnVariableSetup != null && !returnVariableSetup.isEmpty()) {
      builder.append(", setup=[");
      for (int i = 0; i < returnVariableSetup.size(); i++) {
        if (i > 0) {
          builder.append(" ");
        }
        builder.append(returnVariableSetup.get(i));
      }
      builder.append("]");
    }

    return builder.append("]").toString();
  }
}