package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.objectweb.asm.ClassWriter;

/**
 * Specialized generator for EK9 null coalescing operator (??).
 * Handles CONTROL_FLOW_CHAIN with chain_type: "NULL_COALESCING_OPERATOR".
 * <p>
 * EK9 Null Coalescing Operator Pattern: {@code lhs ?? rhs}
 * </p>
 * <ul>
 *   <li>If LHS is null → returns RHS</li>
 *   <li>If LHS is not null → returns LHS (regardless of set/unset state)</li>
 * </ul>
 * <p>
 * Key Difference from Elvis Operator (:?):
 * </p>
 * <ul>
 *   <li>?? checks ONLY null (IS_NULL instruction)</li>
 *   <li>:? checks BOTH null AND set (IS_NULL + IS_SET instructions)</li>
 * </ul>
 * <p>
 * IR Structure:
 * </p>
 * <pre>
 * CONTROL_FLOW_CHAIN [chain_type: "NULL_COALESCING_OPERATOR"]
 *   condition_chain:
 *     [case_type: "NULL_CHECK"]
 *       condition_evaluation: [LOAD lhs, IS_NULL lhs]
 *       primitive_condition: temp_is_null
 *       body_evaluation: [LOAD rhs]  // Returns RHS if null
 *       body_result: rhs
 *   default_body_evaluation: [LOAD lhs]  // Returns LHS if not null
 *   default_result: lhs
 * </pre>
 * <p>
 * Stack Frame Invariant: All paths arrive at 'end' label with empty stack.
 * Result is stored in local variable (overall_result).
 * </p>
 */
final class NullCoalescingOperatorAsmGenerator extends AbstractControlFlowAsmGenerator {

  NullCoalescingOperatorAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                     final OutputVisitor outputVisitor,
                                     final ClassWriter classWriter,
                                     final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter, context);
  }

  /**
   * Generate bytecode for null coalescing operator (??).
   * <p>
   * Bytecode Pattern:
   * </p>
   * <pre>
   * // Evaluate condition: LOAD lhs, IS_NULL
   * ALOAD lhs_index
   * IFNONNULL not_null_label
   *
   * // NULL case: load RHS
   * ALOAD rhs_index
   * ASTORE result_index
   * GOTO end_label
   *
   * not_null_label:
   * // NOT NULL case: load LHS
   * ALOAD lhs_index
   * ASTORE result_index
   *
   * end_label:
   * // result now holds correct value
   * </pre>
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (result in local variable)
   * </p>
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with NULL_COALESCING_OPERATOR type
   */
  public void generate(final ControlFlowChainInstr instr) {
    // Create end label where all paths converge
    // Use scope ID from instruction (now unique per operator instance)
    final var endLabel = createControlFlowLabel("null_end", instr.getScopeId());

    // Process condition chain (typically one NULL_CHECK case for null coalescing)
    for (var conditionCase : instr.getConditionChain()) {
      // caseScopeId is now unique (operator has its own scope)
      // No need for workarounds - just use caseScopeId directly
      final var nextLabel = createControlFlowLabel("null_next", conditionCase.caseScopeId());

      // Use processConditionCase which handles label placement correctly
      processConditionCase(conditionCase, nextLabel, endLabel, instr.getResult());
      // Stack: empty at nextLabel (processConditionCase places it internally)
    }

    // Default case: LHS is not null, return LHS
    // NOTE: For null coalescing, defaultBodyEvaluation is EMPTY because LHS was already evaluated
    // We only need to copy the defaultResult (lhsTemp) to the overall result
    if (instr.getDefaultBodyEvaluation() != null && !instr.getDefaultBodyEvaluation().isEmpty()) {
      // Process default body evaluation if any instructions exist (leaves stack empty)
      processBodyEvaluation(instr.getDefaultBodyEvaluation());
      // Stack: empty
    }

    // Copy default result to overall result (LHS value already evaluated)
    if (instr.getResult() != null && instr.getDefaultResult() != null) {
      copyResultVariable(instr.getDefaultResult(), instr.getResult());
      // Stack: empty
    }

    // End label: both null path (via GOTO) and not-null path (fall-through) arrive here
    placeLabel(endLabel);
    // Stack: empty (guaranteed by both paths)
    // Result is in local variable instr.getResult()
  }
}
