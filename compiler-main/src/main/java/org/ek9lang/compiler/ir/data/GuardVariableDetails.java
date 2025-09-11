package org.ek9lang.compiler.ir.data;

import java.util.List;
import javax.annotation.Nonnull;
import org.ek9lang.compiler.ir.instructions.IRInstr;

/**
 * Record containing guard variable management information for CONTROL_FLOW_CHAIN constructs.
 * <p>
 * Guard variables are used in EK9 control flow constructs like:
 * - if/when with guards: "when var &lt;- expr with condition"
 * - switch with guards: "switch var := expr with var"
 * - for loops with guards: "for var ?= expr then iterator"
 * </p>
 * <p>
 * Provides SSA conversion hints to LLVM backend for proper phi node placement
 * and scope management for guard variable lifetime.
 * </p>
 */
public record GuardVariableDetails(
    /*
     * List of guard variable names that will be reassigned during condition evaluation.
     * Provides SSA conversion hints to LLVM backend for phi node placement.
     * Empty list if no guard variables are used.
     */
    List<String> guardVariables,

    /*
     * Instructions to setup guard variables before condition evaluation.
     * Contains declarations and initial assignments for guard variables.
     * Empty list if no guard setup is needed.
     */
    List<IRInstr> guardScopeSetup,

    /*
     * Scope ID for guard variable lifetime management.
     * Guard variables live in this scope and are accessible to all conditions and bodies.
     * null if no guard variables are used.
     */
    String guardScopeId,

    /*
     * Scope ID for shared condition evaluation.
     * All condition evaluations share this scope for optimization opportunities.
     * null if conditions should use individual scopes.
     */
    String conditionScopeId
) {

  /**
   * Create empty guard variable details (no guards).
   */
  public static GuardVariableDetails none() {
    return new GuardVariableDetails(List.of(), List.of(), null, null);
  }

  /**
   * Create guard variable details with all components.
   */
  public static GuardVariableDetails create(
      List<String> guardVariables,
      List<IRInstr> guardScopeSetup,
      String guardScopeId,
      String conditionScopeId) {
    return new GuardVariableDetails(guardVariables, guardScopeSetup, guardScopeId, conditionScopeId);
  }

  /**
   * Check if this has guard variables.
   */
  public boolean hasGuardVariables() {
    return guardVariables != null && !guardVariables.isEmpty();
  }

  /**
   * Check if this has a guard scope.
   */
  public boolean hasGuardScope() {
    return guardScopeId != null;
  }

  /**
   * Check if this has a shared condition scope.
   */
  public boolean hasSharedConditionScope() {
    return conditionScopeId != null;
  }

  /**
   * Check if this guard details object is empty (no guard features).
   */
  public boolean isEmpty() {
    return !hasGuardVariables() && !hasGuardScope() && !hasSharedConditionScope();
  }

  /**
   * IR-optimized toString following EK9's bracket-only, no-indentation format.
   * Returns empty string if no guard information to prevent rightward drift.
   */
  @Override
  @Nonnull
  public String toString() {
    if (isEmpty()) {
      return "";
    }

    var builder = new StringBuilder("[");
    boolean first = true;

    if (hasGuardVariables()) {
      builder.append("vars=").append(guardVariables);
      first = false;
    }

    if (guardScopeId != null) {
      if (!first) {
        builder.append(", ");
      }
      builder.append("guardScope=").append(guardScopeId);
      first = false;
    }

    if (conditionScopeId != null) {
      if (!first) {
        builder.append(", ");
      }
      builder.append("condScope=").append(conditionScopeId);
      first = false;
    }

    if (guardScopeSetup != null && !guardScopeSetup.isEmpty()) {
      if (!first) {
        builder.append(", ");
      }
      builder.append("setup=").append(guardScopeSetup.size()).append("instr");
    }

    return builder.append("]").toString();
  }
}