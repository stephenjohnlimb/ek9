package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.objectweb.asm.ClassWriter;

/**
 * Specialized generator for EK9 Elvis coalescing operator (:?).
 * Handles CONTROL_FLOW_CHAIN with chain_type: "ELVIS_COALESCING_OPERATOR".
 * <p>
 * EK9 Elvis Coalescing Operator Pattern: {@code lhs :? rhs}
 * </p>
 * <ul>
 *   <li>If LHS is null → returns RHS</li>
 *   <li>If LHS is not null BUT unset → returns RHS</li>
 *   <li>If LHS is not null AND set → returns LHS</li>
 * </ul>
 * <p>
 * Key Difference from Null Coalescing (??):
 * </p>
 * <ul>
 *   <li>?? checks ONLY null (IS_NULL instruction) - 1 case</li>
 *   <li>:? checks BOTH null AND set (IS_NULL + IS_SET instructions) - 2 cases</li>
 * </ul>
 * <p>
 * IR Structure:
 * </p>
 * <pre>
 * CONTROL_FLOW_CHAIN [chain_type: "ELVIS_COALESCING_OPERATOR"]
 *   condition_chain:
 *     [case 1: case_type: "NULL_CHECK"]
 *       condition_evaluation: [LOAD lhs, IS_NULL lhs]
 *       primitive_condition: temp_is_null
 *       body_evaluation: [LOAD rhs]  // Returns RHS if null
 *       body_result: rhs
 *     [case 2: case_type: "EXPRESSION"]
 *       condition_evaluation: [CALL lhs._isSet(), CALL result._false()]
 *       condition_result: isSetResult (EK9 Boolean)
 *       primitive_condition: invertedPrimitive (NOT set)
 *       body_evaluation: [LOAD rhs]  // Returns RHS if unset
 *       body_result: rhs
 *   default_body_evaluation: []  // LHS already evaluated
 *   default_result: lhs
 * </pre>
 * <p>
 * Safety: NULL_CHECK always precedes _isSet() call to prevent accessing unallocated memory.
 * </p>
 * <p>
 * Stack Frame Invariant: All paths arrive at 'end' label with empty stack.
 * Result is stored in local variable (overall_result).
 * </p>
 */
final class ElvisCoalescingOperatorAsmGenerator extends AbstractControlFlowAsmGenerator {

  ElvisCoalescingOperatorAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                      final OutputVisitor outputVisitor,
                                      final ClassWriter classWriter,
                                      final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter, context);
  }

  /**
   * Generate bytecode for Elvis coalescing operator (:?).
   * <p>
   * Bytecode Pattern:
   * </p>
   * <pre>
   * // Case 1: Check if LHS is null
   * ALOAD lhs_index
   * IFNONNULL not_null_label
   * // NULL case: load RHS
   * ALOAD rhs_index
   * ASTORE result_index
   * GOTO end_label
   *
   * not_null_label:
   * // Case 2: Check if LHS is set (safe because not null)
   * ALOAD lhs_index
   * INVOKEVIRTUAL _isSet()
   * INVOKEVIRTUAL _false()  // Inverted: if NOT set
   * ILOAD invertedPrimitive_index
   * IFEQ is_set_label
   * // UNSET case: load RHS
   * ALOAD rhs_index
   * ASTORE result_index
   * GOTO end_label
   *
   * is_set_label:
   * // SET case: load LHS
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
   * @param instr CONTROL_FLOW_CHAIN instruction with ELVIS_COALESCING_OPERATOR type
   */
  public void generate(final ControlFlowChainInstr instr) {
    // Create end label where all paths converge
    // Use scope ID from instruction (now unique per operator instance)
    final var endLabel = createControlFlowLabel("elvis_end", instr.getScopeId());

    // Process condition chain (typically 2 cases: NULL_CHECK + EXPRESSION for Elvis)
    for (var conditionCase : instr.getConditionChain()) {
      // Each case now has its own unique caseScopeId
      final var nextLabel = createControlFlowLabel("elvis_next", conditionCase.caseScopeId());

      // Use processConditionCase which handles label placement correctly
      processConditionCase(conditionCase, nextLabel, endLabel, instr.getResult());
      // Stack: empty at nextLabel (processConditionCase places it internally)
    }

    // Default case: LHS is not null AND set, return LHS
    // NOTE: For Elvis coalescing, defaultBodyEvaluation is EMPTY because LHS was already evaluated
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

    // End label: all paths arrive here with empty stack
    placeLabel(endLabel);
    // Stack: empty (guaranteed by all paths)
    // Result is in local variable instr.getResult()
  }
}
