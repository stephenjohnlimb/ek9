package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 * Abstract base class for control flow bytecode generators.
 * Provides common patterns for:
 * - Label creation with consistent naming
 * - Condition evaluation processing
 * - Result variable copying
 * - Stack frame validation helpers
 * <p>
 * All control flow generators (question operator, if/else, switch, loops, try/catch)
 * extend this class to reuse common logic and ensure consistent stack frame handling.
 * </p>
 * <p>
 * Stack Frame Invariant: All helper methods maintain the invariant that the JVM
 * operand stack is empty before and after their execution. Results are always
 * stored in local variables, never left on the stack. This ensures correct
 * stack frame balancing at control flow merge points.
 * </p>
 */
abstract class AbstractControlFlowAsmGenerator extends AbstractAsmGenerator {

  protected AbstractControlFlowAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                            final OutputVisitor outputVisitor,
                                            final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate unique label for control flow construct.
   * Pattern: prefix_uniqueId
   *
   * @param prefix Label prefix identifying the construct type (e.g., "qop", "if", "switch")
   * @param uniqueId Unique identifier from IR (e.g., result variable name, scope ID)
   * @return JVM Label for bytecode generation
   */
  protected Label createControlFlowLabel(final String prefix, final String uniqueId) {
    return getOrCreateLabel(prefix + "_" + uniqueId);
  }

  /**
   * Process condition evaluation instructions via recursive visiting.
   * All instructions leave stack empty (results stored in variables).
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (all results in local variables)
   * </p>
   *
   * @param conditionEvaluation List of IR instructions to evaluate condition
   */
  protected void processConditionEvaluation(final java.util.List<IRInstr> conditionEvaluation) {
    for (var instr : conditionEvaluation) {
      instr.accept(outputVisitor);  // Recursive delegation to OutputVisitor
      // Each instruction maintains stack-empty invariant
    }
    // Post-condition: stack is empty, condition result in local variable
  }

  /**
   * Process body evaluation instructions via recursive visiting.
   * All instructions leave stack empty (results stored in variables).
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (all results in local variables)
   * </p>
   *
   * @param bodyEvaluation List of IR instructions for case/branch body
   */
  protected void processBodyEvaluation(final java.util.List<IRInstr> bodyEvaluation) {
    for (var instr : bodyEvaluation) {
      instr.accept(outputVisitor);  // Recursive delegation to OutputVisitor
      // Each instruction maintains stack-empty invariant
    }
    // Post-condition: stack is empty, body result in local variable
  }

  /**
   * Load primitive condition (int) and branch if false (0).
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (IFEQ consumes the int)
   * </p>
   * <p>
   * Control Flow: If condition is 0 (false), jumps to falseLabel.
   * Otherwise, execution continues to next instruction.
   * </p>
   *
   * @param primitiveCondition Variable name holding primitive int (0 or 1)
   * @param falseLabel Label to jump to if condition is false (0)
   */
  protected void branchIfFalse(final String primitiveCondition, final Label falseLabel) {
    final var conditionIndex = getVariableIndex(primitiveCondition);
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ILOAD, conditionIndex);  // stack: [int]
    getCurrentMethodVisitor().visitJumpInsn(Opcodes.IFEQ, falseLabel);  // stack: [] (consumed by IFEQ)
    // Post-condition: stack is empty
  }

  /**
   * Load primitive condition (int) and branch if true (non-zero).
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (IFNE consumes the int)
   * </p>
   * <p>
   * Control Flow: If condition is non-zero (true), jumps to trueLabel.
   * Otherwise, execution continues to next instruction.
   * </p>
   * <p>
   * Used by logical OR operator for short-circuit optimization:
   * If left operand is true, skip right evaluation and return left.
   * </p>
   *
   * @param primitiveCondition Variable name holding primitive int (0 or 1)
   * @param trueLabel Label to jump to if condition is true (non-zero)
   */
  protected void branchIfTrue(final String primitiveCondition, final Label trueLabel) {
    final var conditionIndex = getVariableIndex(primitiveCondition);
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ILOAD, conditionIndex);  // stack: [int]
    getCurrentMethodVisitor().visitJumpInsn(Opcodes.IFNE, trueLabel);  // stack: [] (consumed by IFNE)
    // Post-condition: stack is empty
  }

  /**
   * Copy result from source variable to destination variable (object reference).
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty
   * </p>
   * <p>
   * Bytecode: ALOAD source_index, ASTORE dest_index
   * </p>
   *
   * @param sourceVar Variable holding the result to copy
   * @param destVar Variable to store the result into
   */
  protected void copyResultVariable(final String sourceVar, final String destVar) {
    final var sourceIndex = getVariableIndex(sourceVar);
    final var destIndex = getVariableIndex(destVar);
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ALOAD, sourceIndex);  // stack: [obj]
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ASTORE, destIndex);  // stack: []
    // Post-condition: stack is empty
  }

  /**
   * Generate unconditional jump to label.
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: control transferred (stack irrelevant after GOTO)
   * </p>
   * <p>
   * Control Flow: Execution continues at the target label.
   * Stack at target must match stack at GOTO (empty in our convention).
   * </p>
   *
   * @param label Target label for unconditional jump
   */
  protected void jumpTo(final Label label) {
    getCurrentMethodVisitor().visitJumpInsn(Opcodes.GOTO, label);
    // Post-condition: control transferred, stack must be empty at target
  }

  /**
   * Place label at current bytecode position.
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack state depends on incoming control flow paths
   * Post-condition: stack state unchanged (label is just a position marker)
   * </p>
   * <p>
   * CRITICAL: All control flow paths reaching this label MUST have the same
   * stack depth. In our convention, all paths arrive with empty stack.
   * </p>
   *
   * @param label Label to place at current position
   */
  protected void placeLabel(final Label label) {
    getCurrentMethodVisitor().visitLabel(label);
    // Stack state: must be consistent across all incoming paths
  }

  /**
   * Process a condition case: evaluate condition, branch, execute body.
   * Common pattern used by question operator, if/else, switch.
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty at both nextCaseLabel and after GOTO
   * </p>
   * <p>
   * Control Flow:
   * 1. Evaluate condition → result in variable, stack empty
   * 2. Branch to nextCaseLabel if false → stack empty at label
   * 3. Execute body → result in variable, stack empty
   * 4. Copy result to overall result → stack empty
   * 5. Jump to endLabel → stack empty at label
   * 6. Place nextCaseLabel → stack empty (from branch in step 2)
   * </p>
   *
   * @param conditionCase IR details for this case
   * @param nextCaseLabel Label to jump if condition is false
   * @param endLabel Label to jump after body execution
   * @param resultVar Overall result variable (null if no result)
   */
  protected void processConditionCase(final ConditionCaseDetails conditionCase,
                                      final Label nextCaseLabel,
                                      final Label endLabel,
                                      final String resultVar) {
    processConditionCaseWithoutLabelPlacement(conditionCase, nextCaseLabel, endLabel, resultVar);

    // 6. Place next case label (reached via branch from step 2 with empty stack)
    placeLabel(nextCaseLabel);
    // Stack: empty (from incoming branch)
  }

  /**
   * Process a condition case WITHOUT placing the nextCaseLabel.
   * Use this when the caller needs to control label placement (e.g., if/else-if chains).
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty after GOTO
   * </p>
   *
   * @param conditionCase IR details for this case
   * @param nextCaseLabel Label to jump if condition is false
   * @param endLabel Label to jump after body execution
   * @param resultVar Overall result variable (null if no result)
   */
  protected void processConditionCaseWithoutLabelPlacement(final ConditionCaseDetails conditionCase,
                                                            final Label nextCaseLabel,
                                                            final Label endLabel,
                                                            final String resultVar) {
    // 1. Evaluate condition (leaves stack empty)
    processConditionEvaluation(conditionCase.conditionEvaluation());
    // Stack: empty

    // 2. Branch if condition is false
    branchIfFalse(conditionCase.primitiveCondition(), nextCaseLabel);
    // Stack: empty (both paths - continue or branch)

    // 3. Execute body (leaves stack empty)
    processBodyEvaluation(conditionCase.bodyEvaluation());
    // Stack: empty

    // 4. Copy body result to overall result (if exists)
    if (resultVar != null && conditionCase.bodyResult() != null) {
      copyResultVariable(conditionCase.bodyResult(), resultVar);
      // Stack: empty
    }

    // 5. Jump to end
    jumpTo(endLabel);
    // Stack: empty at endLabel
    // NOTE: nextCaseLabel is NOT placed by this method - caller must place it
  }
}
