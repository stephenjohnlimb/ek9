package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.objectweb.asm.ClassWriter;

/**
 * Specialized generator for EK9 ternary operator.
 * Handles CONTROL_FLOW_CHAIN with chain_type: "TERNARY_OPERATOR".
 * <p>
 * EK9 Ternary Operator Pattern: {@code condition <- thenValue : elseValue}
 * </p>
 * <ul>
 *   <li>If condition is true → returns thenValue</li>
 *   <li>If condition is false → returns elseValue</li>
 * </ul>
 * <p>
 * IR Structure:
 * </p>
 * <pre>
 * CONTROL_FLOW_CHAIN [chain_type: "TERNARY_OPERATOR"]
 *   condition_chain:
 *     [case 1: case_type: "EXPRESSION"]
 *       condition_evaluation: [LOAD condition, CALL _true()]
 *       condition_result: conditionTemp (EK9 Boolean)
 *       primitive_condition: primitiveBoolean (Java boolean)
 *       body_evaluation: [evaluate thenValue]
 *       body_result: thenTemp
 *   default_body_evaluation: [evaluate elseValue]
 *   default_result: elseTemp
 * </pre>
 * <p>
 * Bytecode Pattern:
 * </p>
 * <pre>
 * // Condition evaluation (produces primitive boolean)
 * ALOAD condition_index
 * INVOKEVIRTUAL _true()
 * ILOAD primitive_index
 * IFEQ else_label
 *
 * // True branch: evaluate thenValue
 * [then_evaluation_instructions]
 * ALOAD then_result_index
 * ASTORE result_index
 * GOTO end_label
 *
 * else_label:
 * // False branch: evaluate elseValue
 * [else_evaluation_instructions]
 * ALOAD else_result_index
 * ASTORE result_index
 *
 * end_label:
 * // result now holds correct value
 * </pre>
 * <p>
 * Stack Frame Invariant: All paths arrive at 'end' label with empty stack.
 * Result is stored in local variable (overall_result).
 * </p>
 */
final class TernaryOperatorAsmGenerator extends AbstractControlFlowAsmGenerator {

  TernaryOperatorAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                              final OutputVisitor outputVisitor,
                              final ClassWriter classWriter,
                              final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter, context);
  }

  /**
   * Generate bytecode for ternary operator.
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (result in local variable)
   * </p>
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with TERNARY_OPERATOR type
   */
  public void generate(final ControlFlowChainInstr instr) {
    // Create end label where all paths converge
    final var endLabel = createControlFlowLabel("ternary_end", instr.getScopeId());

    // Process true branch (exactly one condition case for ternary)
    final var conditionCase = instr.getConditionChain().getFirst();
    final var elseLabel = createControlFlowLabel("ternary_else", conditionCase.caseScopeId());

    // Process condition and true branch
    // If condition is true, execute body and jump to end
    // If condition is false, fall through to elseLabel
    processConditionCase(conditionCase, elseLabel, endLabel, instr.getResult());
    // Stack: empty at elseLabel

    // Place else label for false branch
    placeLabel(elseLabel);
    // Stack: empty

    // Process false branch (default case)
    if (instr.getDefaultBodyEvaluation() != null && !instr.getDefaultBodyEvaluation().isEmpty()) {
      processBodyEvaluation(instr.getDefaultBodyEvaluation());
      // Stack: empty
    }

    // Copy else result to overall result (if different)
    // NOTE: Skip copy if else branch already stored directly to result variable
    if (instr.getResult() != null && instr.getDefaultResult() != null
        && !instr.getDefaultResult().equals(instr.getResult())) {
      copyResultVariable(instr.getDefaultResult(), instr.getResult());
      // Stack: empty
    }

    // End label: all paths arrive here with empty stack
    placeLabel(endLabel);
    // Stack: empty (guaranteed by all paths)
    // Result is in local variable instr.getResult()
  }
}
