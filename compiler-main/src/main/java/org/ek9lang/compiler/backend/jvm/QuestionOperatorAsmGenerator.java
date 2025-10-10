package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.objectweb.asm.ClassWriter;

/**
 * Specialized generator for EK9 question operator (?).
 * Handles CONTROL_FLOW_CHAIN with chain_type: "QUESTION_OPERATOR".
 * <p>
 * EK9 Question Operator Pattern: {@code operand?}
 * </p>
 * <ul>
 *   <li>If operand is null → returns Boolean(false)</li>
 *   <li>If operand is not null → returns operand._isSet()</li>
 * </ul>
 *
 * <p>
 * Stack Frame Invariant: All paths arrive at 'end' label with empty stack.
 * Result is stored in local variable (overall_result).
 * </p>
 */
public final class QuestionOperatorAsmGenerator extends AbstractControlFlowAsmGenerator {

  public QuestionOperatorAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                      final OutputVisitor outputVisitor,
                                      final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate bytecode for question operator.
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (result in local variable)
   * </p>
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with QUESTION_OPERATOR type
   */
  public void generate(final ControlFlowChainInstr instr) {
    // Create end label where all paths converge
    // Use result variable name to ensure uniqueness across multiple question operators
    final var endLabel = createControlFlowLabel("qop_end", instr.getResult());

    // Process condition chain (typically one NULL_CHECK case for question operator)
    for (var conditionCase : instr.getConditionChain()) {
      // Make label unique by including result variable name + scope ID
      // This prevents label collision when multiple ? operators exist in same method
      final var nextLabel = createControlFlowLabel(
          "qop_next_" + instr.getResult(), conditionCase.caseScopeId());

      // Use shared helper: evaluate condition, branch, execute body, copy result, jump to end
      // All operations maintain stack-empty invariant
      processConditionCase(conditionCase, nextLabel, endLabel, instr.getResult());
      // Stack: empty at nextLabel (from branch in processConditionCase)
    }

    // Default case: operand is not null, call operand._isSet()
    if (instr.hasDefaultCase()) {
      // Process default body evaluation (leaves stack empty)
      processBodyEvaluation(instr.getDefaultBodyEvaluation());
      // Stack: empty

      // Copy default result to overall result (if exists)
      if (instr.getResult() != null && instr.getDefaultResult() != null) {
        copyResultVariable(instr.getDefaultResult(), instr.getResult());
        // Stack: empty
      }
    }

    // End label: both true path (via GOTO) and false path (fall-through) arrive here
    placeLabel(endLabel);
    // Stack: empty (guaranteed by both paths)
    // Result is in local variable instr.getResult()
  }
}
