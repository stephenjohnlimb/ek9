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
 * - while/do-while with guards: "while var &lt;- expr then condition"
 * </p>
 * <p>
 * Provides SSA conversion hints to LLVM backend for proper phi node placement
 * and scope management for guard variable lifetime.
 * </p>
 * <p>
 * For WHILE/DO-WHILE loops, the guard entry check is evaluated ONCE at loop entry.
 * If the check fails, the entire loop is skipped. The condition is then evaluated
 * on each iteration WITHOUT the guard check.
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
     * Instructions to check if guard variable is valid (NOT NULL AND IS SET).
     * For loops, this check is executed ONCE at entry. If it fails, the loop is skipped.
     * Empty list if no guard entry check is needed.
     */
    List<IRInstr> guardEntryCheck,

    /*
     * Primitive boolean variable name containing result of guard entry check.
     * Used for conditional branching to skip the loop if guard is invalid.
     * null if no guard entry check is needed.
     */
    String guardEntryCheckPrimitive,

    /*
     * EK9 Boolean variable name containing result of guard entry check.
     * Used for condition_result in IF_ELSE_IF wrapper to enable backend optimizations.
     * null if no guard entry check is needed.
     */
    String guardEntryCheckResult,

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
    return new GuardVariableDetails(List.of(), List.of(), List.of(), null, null, null, null);
  }

  /**
   * Create guard variable details for IF/SWITCH (no entry check needed - evaluates with condition).
   */
  public static GuardVariableDetails create(
      List<String> guardVariables,
      List<IRInstr> guardScopeSetup,
      String guardScopeId,
      String conditionScopeId) {
    return new GuardVariableDetails(guardVariables, guardScopeSetup, List.of(), null, null, guardScopeId, conditionScopeId);
  }

  /**
   * Create guard variable details for WHILE/DO-WHILE loops (with entry check).
   * The entry check is evaluated ONCE at loop entry. If it fails, the loop is skipped.
   *
   * @param guardVariables           List of guard variable names
   * @param guardScopeSetup          Instructions to setup guard variables
   * @param guardEntryCheck          Instructions for guard check (IS_NULL + _isSet)
   * @param guardEntryCheckPrimitive Primitive boolean for branching
   * @param guardEntryCheckResult    EK9 Boolean result for condition_result in IF wrapper
   * @param guardScopeId             Scope ID for guard variable lifetime
   */
  public static GuardVariableDetails createWithEntryCheck(
      List<String> guardVariables,
      List<IRInstr> guardScopeSetup,
      List<IRInstr> guardEntryCheck,
      String guardEntryCheckPrimitive,
      String guardEntryCheckResult,
      String guardScopeId) {
    return new GuardVariableDetails(guardVariables, guardScopeSetup, guardEntryCheck,
        guardEntryCheckPrimitive, guardEntryCheckResult, guardScopeId, null);
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
   * Check if this has a guard entry check (for WHILE/DO-WHILE loops).
   */
  public boolean hasGuardEntryCheck() {
    return guardEntryCheck != null && !guardEntryCheck.isEmpty() && guardEntryCheckPrimitive != null;
  }

  /**
   * Check if this guard details object is empty (no guard features).
   */
  public boolean isEmpty() {
    return !hasGuardVariables() && !hasGuardScope() && !hasSharedConditionScope() && !hasGuardEntryCheck();
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