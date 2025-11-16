package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.objectweb.asm.ClassWriter;

/**
 * Specialized generator for EK9 comparison coalescing operators ({@code <?, >?, <=?, >=?}).
 * Handles CONTROL_FLOW_CHAIN with chain_type: "LESS_THAN_COALESCING_OPERATOR",
 * "GREATER_THAN_COALESCING_OPERATOR", "LESS_EQUAL_COALESCING_OPERATOR",
 * or "GREATER_EQUAL_COALESCING_OPERATOR".
 * <p>
 * EK9 Comparison Coalescing Operator Patterns:
 * </p>
 * <ul>
 *   <li>{@code lhs <? rhs} - Returns lesser value, gracefully handling null/unset</li>
 *   <li>{@code lhs >? rhs} - Returns greater value, gracefully handling null/unset</li>
 *   <li>{@code lhs <=? rhs} - Returns LHS if less-or-equal, else RHS, handling null/unset</li>
 *   <li>{@code lhs >=? rhs} - Returns LHS if greater-or-equal, else RHS, handling null/unset</li>
 * </ul>
 * <p>
 * Comparison Logic for {@code lhs <? rhs}:
 * </p>
 * <ol>
 *   <li>If LHS is null → returns RHS</li>
 *   <li>If LHS is not null BUT unset → returns RHS</li>
 *   <li>If RHS is null → returns LHS</li>
 *   <li>If RHS is not null BUT unset → returns LHS</li>
 *   <li>If LHS &lt; RHS → returns LHS (lesser value)</li>
 *   <li>Otherwise → returns RHS</li>
 * </ol>
 * <p>
 * Key Differences from Other Coalescing Operators:
 * </p>
 * <ul>
 *   <li>Null coalescing (??) - 1 case (NULL check only)</li>
 *   <li>Elvis coalescing (:?) - 2 cases (NULL + IS_SET on LHS only)</li>
 *   <li>Comparison coalescing (&lt;?, &gt;?, &lt;=?, &gt;=?) - 5 cases (NULL + IS_SET on BOTH operands + comparison)</li>
 * </ul>
 * <p>
 * IR Structure (example for {@code <? } operator):
 * </p>
 * <pre>
 * CONTROL_FLOW_CHAIN [chain_type: "LESS_THAN_COALESCING_OPERATOR"]
 *   condition_chain:
 *     [case 1: case_type: "NULL_CHECK"]
 *       condition_evaluation: [LOAD lhs, IS_NULL lhs]
 *       primitive_condition: lhs_is_null
 *       body_evaluation: [LOAD rhs]
 *       body_result: rhs
 *     [case 2: case_type: "EXPRESSION"]
 *       condition_evaluation: [CALL lhs._isSet(), CALL result._false()]
 *       condition_result: lhs_isSet (EK9 Boolean)
 *       primitive_condition: lhs_not_set (inverted)
 *       body_evaluation: [LOAD rhs]
 *       body_result: rhs
 *     [case 3: case_type: "NULL_CHECK"]
 *       condition_evaluation: [LOAD rhs, IS_NULL rhs]
 *       primitive_condition: rhs_is_null
 *       body_evaluation: []  // LHS already loaded
 *       body_result: lhs
 *     [case 4: case_type: "EXPRESSION"]
 *       condition_evaluation: [CALL rhs._isSet(), CALL result._false()]
 *       condition_result: rhs_isSet (EK9 Boolean)
 *       primitive_condition: rhs_not_set (inverted)
 *       body_evaluation: []  // LHS already loaded
 *       body_result: lhs
 *     [case 5: case_type: "EXPRESSION"]
 *       condition_evaluation: [CALL lhs._lt(rhs), CALL result._true()]
 *       condition_result: comparison (EK9 Boolean)
 *       primitive_condition: is_less_than
 *       body_evaluation: []  // LHS already loaded
 *       body_result: lhs
 *   default_body_evaluation: [LOAD rhs]
 *   default_result: rhs
 * </pre>
 * <p>
 * Safety: NULL_CHECK cases always precede _isSet() calls to prevent accessing unallocated memory.
 * </p>
 * <p>
 * Stack Frame Invariant: All paths arrive at 'end' label with empty stack.
 * Result is stored in local variable (overall_result).
 * </p>
 */
final class ComparisonCoalescingOperatorAsmGenerator extends AbstractControlFlowAsmGenerator {

  ComparisonCoalescingOperatorAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                          final OutputVisitor outputVisitor,
                                          final ClassWriter classWriter,
                                          final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter, context);
  }

  /**
   * Generate bytecode for comparison coalescing operators ({@code <?, >?, <=?, >=?}).
   * <p>
   * Bytecode Pattern (example for {@code <? }):
   * </p>
   * <pre>
   * // Case 1: Check if LHS is null
   * ALOAD lhs_index
   * IFNONNULL lhs_not_null_label
   * // NULL case: load RHS
   * ALOAD rhs_index
   * ASTORE result_index
   * GOTO end_label
   *
   * lhs_not_null_label:
   * // Case 2: Check if LHS is set
   * ALOAD lhs_index
   * INVOKEVIRTUAL _isSet()
   * INVOKEVIRTUAL _false()  // Inverted: if NOT set
   * ILOAD invertedPrimitive_index
   * IFEQ lhs_is_set_label
   * // UNSET case: load RHS
   * ALOAD rhs_index
   * ASTORE result_index
   * GOTO end_label
   *
   * lhs_is_set_label:
   * // Case 3: Check if RHS is null
   * ALOAD rhs_index
   * IFNONNULL rhs_not_null_label
   * // NULL case: load LHS
   * ALOAD lhs_index
   * ASTORE result_index
   * GOTO end_label
   *
   * rhs_not_null_label:
   * // Case 4: Check if RHS is set
   * ALOAD rhs_index
   * INVOKEVIRTUAL _isSet()
   * INVOKEVIRTUAL _false()  // Inverted: if NOT set
   * ILOAD invertedPrimitive_index
   * IFEQ rhs_is_set_label
   * // UNSET case: load LHS
   * ALOAD lhs_index
   * ASTORE result_index
   * GOTO end_label
   *
   * rhs_is_set_label:
   * // Case 5: Perform comparison (LHS &lt; RHS)
   * ALOAD lhs_index
   * ALOAD rhs_index
   * INVOKEVIRTUAL _lt(RHS_type)
   * INVOKEVIRTUAL _true()
   * ILOAD comparison_index
   * IFEQ comparison_false_label
   * // COMPARISON TRUE: load LHS
   * ALOAD lhs_index
   * ASTORE result_index
   * GOTO end_label
   *
   * comparison_false_label:
   * // COMPARISON FALSE: load RHS (default case)
   * ALOAD rhs_index
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
   * @param instr CONTROL_FLOW_CHAIN instruction with comparison coalescing type
   */
  public void generate(final ControlFlowChainInstr instr) {
    // Create end label where all paths converge
    // Use scope ID from instruction (unique per operator instance)
    final var chainType = instr.getChainType();
    final var labelPrefix = getLabelPrefix(chainType);
    final var endLabel = createControlFlowLabel(labelPrefix + "_end", instr.getScopeId());

    // Process condition chain (typically 5 cases for comparison coalescing)
    for (var conditionCase : instr.getConditionChain()) {
      // Each case now has its own unique caseScopeId
      final var nextLabel = createControlFlowLabel(labelPrefix + "_next", conditionCase.caseScopeId());

      // Use processConditionCase which handles label placement correctly
      processConditionCase(conditionCase, nextLabel, endLabel, instr.getResult());
      // Stack: empty at nextLabel (processConditionCase places it internally)
    }

    // Default case: comparison was false, return RHS (or comparison-specific default)
    if (instr.getDefaultBodyEvaluation() != null && !instr.getDefaultBodyEvaluation().isEmpty()) {
      // Process default body evaluation if any instructions exist (leaves stack empty)
      processBodyEvaluation(instr.getDefaultBodyEvaluation());
      // Stack: empty
    }

    // Copy default result to overall result
    if (instr.getResult() != null && instr.getDefaultResult() != null) {
      copyResultVariable(instr.getDefaultResult(), instr.getResult());
      // Stack: empty
    }

    // End label: all paths arrive here with empty stack
    placeLabel(endLabel);
    // Stack: empty (guaranteed by all paths)
    // Result is in local variable instr.getResult()
  }

  /**
   * Get label prefix based on comparison coalescing operator type.
   *
   * @param chainType The chain type from IR
   * @return Label prefix for bytecode labels
   */
  private String getLabelPrefix(final String chainType) {
    return switch (chainType) {
      case "LESS_THAN_COALESCING_OPERATOR" -> "lt_coalesce";
      case "GREATER_THAN_COALESCING_OPERATOR" -> "gt_coalesce";
      case "LESS_EQUAL_COALESCING_OPERATOR" -> "lte_coalesce";
      case "GREATER_EQUAL_COALESCING_OPERATOR" -> "gte_coalesce";
      default -> "cmp_coalesce";
    };
  }
}
