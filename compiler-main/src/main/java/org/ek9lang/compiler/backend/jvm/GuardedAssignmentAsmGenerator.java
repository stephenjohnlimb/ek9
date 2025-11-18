package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.objectweb.asm.ClassWriter;

/**
 * Specialized generator for EK9 guarded assignment (:=?).
 * Handles CONTROL_FLOW_CHAIN with chain_type: "GUARDED_ASSIGNMENT".
 * <p>
 * EK9 Guarded Assignment Pattern: {@code variable :=? value}
 * </p>
 * <ul>
 *   <li>Assigns value to variable only if variable is null or not set</li>
 *   <li>Prevents overwriting already-initialized variables</li>
 *   <li>Useful for default value initialization</li>
 * </ul>
 * <p>
 * Current Status: NOT YET IMPLEMENTED (throws CompilerException).
 * </p>
 * <p>
 * Implementation Plan (after IR generator is finalized):
 * </p>
 * <ul>
 *   <li>Evaluate condition (variable? returns false if null/unset)</li>
 *   <li>If condition false: execute assignment</li>
 *   <li>If condition true: skip assignment</li>
 *   <li>Follow pattern similar to QuestionOperatorAsmGenerator</li>
 * </ul>
 * <p>
 * Blocked By: IR generation for GUARDED_ASSIGNMENT must be finalized and tested first.
 * The IR generator (ControlFlowChainGenerator.generateGuardedAssignment) exists but
 * bytecode generation is deferred pending complete testing of question operator foundation.
 * </p>
 */
final class GuardedAssignmentAsmGenerator extends AbstractControlFlowAsmGenerator {

  GuardedAssignmentAsmGenerator(final ConstructTargetTuple constructTargetTuple, final OutputVisitor outputVisitor,
                                final ClassWriter classWriter, final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter, context);
  }

  /**
   * Generate bytecode for guarded assignment.
   * <p>
   * EK9 Pattern: {@code variable :=? value}
   * </p>
   * <p>
   * Semantic Flow:
   * </p>
   * <ol>
   *   <li>Check if target variable is unset (null or _isSet() returns false)</li>
   *   <li>If UNSET: evaluate RHS expression and assign to target</li>
   *   <li>If SET: skip assignment (fall through to end)</li>
   * </ol>
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (assignment stored in variable)
   * </p>
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with GUARDED_ASSIGNMENT type
   */
  public void generate(final ControlFlowChainInstr instr) {
    // Create end label where both paths (assign/skip) converge
    // Use scopeId for uniqueness (MANDATORY pattern per AbstractControlFlowAsmGenerator)
    final var endLabel = createControlFlowLabel("guarded_assignment_end", instr.getScopeId());

    // Process condition chain (typically one case: "is variable unset?")
    for (var conditionCase : instr.getConditionChain()) {
      // 1. Evaluate condition: "is target variable unset?"
      // This generates: IS_NULL check + _isSet() call + _negate() + _true()
      // Leaves primitive boolean (0/1) in primitiveCondition variable
      processConditionEvaluation(conditionCase.conditionEvaluation());
      // Stack: empty

      // 2. Branch if condition is FALSE (variable is SET, skip assignment)
      branchIfFalse(conditionCase.primitiveCondition(), endLabel);
      // Stack: empty (both paths - continue or branch)

      // 3. Execute body: evaluate RHS and assign to target (only if condition was TRUE)
      // Body contains: function call, STORE to target variable
      processBodyEvaluation(conditionCase.bodyEvaluation());
      // Stack: empty

      // Fall through to end label (assignment completed)
    }

    // End label: both paths (assigned/skipped) arrive here
    placeLabel(endLabel);
    // Stack: empty (guaranteed by both paths)
    // Target variable now contains new value (if was unset) or original value (if was set)
  }
}
