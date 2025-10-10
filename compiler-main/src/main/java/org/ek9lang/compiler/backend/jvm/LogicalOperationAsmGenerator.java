package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.LogicalOperationInstr;
import org.ek9lang.core.CompilerException;
import org.objectweb.asm.ClassWriter;

/**
 * Specialized generator for EK9 logical operations (AND, OR) with short-circuit evaluation.
 * Handles LOGICAL_AND_BLOCK and LOGICAL_OR_BLOCK IR instructions.
 * <p>
 * EK9 Logical AND Pattern: {@code left and right}
 * </p>
 * <ul>
 *   <li>If left is false → short-circuit: return left (false) without evaluating right</li>
 *   <li>If left is true → full evaluation: evaluate right and return left._and(right)</li>
 * </ul>
 * <p>
 * EK9 Logical OR Pattern: {@code left or right}
 * </p>
 * <ul>
 *   <li>If left is true → short-circuit: return left (true) without evaluating right</li>
 *   <li>If left is false → full evaluation: evaluate right and return left._or(right)</li>
 * </ul>
 * <p>
 * Stack Frame Invariant: All paths arrive at end label with empty stack.
 * Result is stored in local variable (instruction result variable).
 * </p>
 * <p>
 * IR Structure:
 * </p>
 * <pre>
 * _tempN = LOGICAL_AND_BLOCK / LOGICAL_OR_BLOCK
 *   left_evaluation → produces left_operand (Boolean) and left_condition (primitive int from _true())
 *   right_evaluation → produces right_operand (Boolean)
 *   result_computation → produces logical_result (Boolean from _and() or _or())
 * </pre>
 * <p>
 * Bytecode generator populates instruction result (_tempN) with either:
 * - left_operand (short-circuit case)
 * - logical_result (full evaluation case)
 * </p>
 */
final class LogicalOperationAsmGenerator extends AbstractControlFlowAsmGenerator {

  LogicalOperationAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                               final OutputVisitor outputVisitor,
                               final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate bytecode for logical operation (AND or OR).
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (result in local variable)
   * </p>
   *
   * @param instr LOGICAL_AND_BLOCK or LOGICAL_OR_BLOCK instruction
   */
  public void generate(final LogicalOperationInstr instr) {
    switch (instr.getOperation()) {
      case AND -> generateAndOperation(instr);
      case OR -> generateOrOperation(instr);
      default -> throw new CompilerException(
          "Unsupported logical operation: " + instr.getOperation());
    }
  }

  /**
   * Generate bytecode for AND operation with short-circuit optimization.
   * <p>
   * Short-circuit rule: If left is false, return left without evaluating right.
   * </p>
   * <p>
   * Bytecode Pattern:
   * </p>
   * <pre>
   * 1. Process left_evaluation → left_operand (Boolean), left_condition (int)
   * 2. ILOAD left_condition, IFEQ shortCircuit → if 0 (false), skip right
   * 3. Process right_evaluation → right_operand (Boolean)
   * 4. Process result_computation → logical_result (_and() result)
   * 5. Copy logical_result → instruction result
   * 6. GOTO end
   * 7. shortCircuit: Copy left_operand → instruction result
   * 8. end: (both paths converge, stack empty)
   * </pre>
   *
   * @param instr LOGICAL_AND_BLOCK instruction
   */
  private void generateAndOperation(final LogicalOperationInstr instr) {
    // Create unique labels using result variable name
    final var shortCircuitLabel = createControlFlowLabel("and_short", instr.getResult());
    final var endLabel = createControlFlowLabel("and_end", instr.getResult());

    // 1. Evaluate left operand and get primitive condition
    processConditionEvaluation(instr.getLeftEvaluationInstructions());
    // Stack: empty, variables: left_operand (Boolean), left_condition (int)

    // 2. Branch if left is false (short-circuit for AND)
    branchIfFalse(instr.getCondition(), shortCircuitLabel);
    // Stack: empty (both paths - continue if true, branch if false)

    // 3. Full evaluation path: left was true, evaluate right operand
    processConditionEvaluation(instr.getRightEvaluationInstructions());
    // Stack: empty, variables: right_operand (Boolean)

    // 4. Call _and() operator
    processConditionEvaluation(instr.getResultComputationInstructions());
    // Stack: empty, variables: logical_result (Boolean from _and())

    // 5. Copy logical_result to instruction result variable
    copyResultVariable(instr.getLogicalResult(), instr.getResult());
    // Stack: empty

    // 6. Jump over short-circuit path
    jumpTo(endLabel);
    // Stack: empty at end label

    // 7. Short-circuit label: left was false, use left as result
    placeLabel(shortCircuitLabel);
    // Stack: empty (from IFEQ branch)

    // 8. Copy left_operand to instruction result (short-circuit: false AND anything = false)
    copyResultVariable(instr.getLeftOperand(), instr.getResult());
    // Stack: empty

    // 9. End label: both paths converge with empty stack
    placeLabel(endLabel);
    // Stack: empty
    // instruction result now contains final Boolean (either left_operand or logical_result)
  }

  /**
   * Generate bytecode for OR operation with short-circuit optimization.
   * <p>
   * Short-circuit rule: If left is true, return left without evaluating right.
   * </p>
   * <p>
   * Bytecode Pattern:
   * </p>
   * <pre>
   * 1. Process left_evaluation → left_operand (Boolean), left_condition (int)
   * 2. ILOAD left_condition, IFNE shortCircuit → if 1 (true), skip right
   * 3. Process right_evaluation → right_operand (Boolean)
   * 4. Process result_computation → logical_result (_or() result)
   * 5. Copy logical_result → instruction result
   * 6. GOTO end
   * 7. shortCircuit: Copy left_operand → instruction result
   * 8. end: (both paths converge, stack empty)
   * </pre>
   *
   * @param instr LOGICAL_OR_BLOCK instruction
   */
  private void generateOrOperation(final LogicalOperationInstr instr) {
    // Create unique labels using result variable name
    final var shortCircuitLabel = createControlFlowLabel("or_short", instr.getResult());
    final var endLabel = createControlFlowLabel("or_end", instr.getResult());

    // 1. Evaluate left operand and get primitive condition
    processConditionEvaluation(instr.getLeftEvaluationInstructions());
    // Stack: empty, variables: left_operand (Boolean), left_condition (int)

    // 2. Branch if left is true (short-circuit for OR) - IFNE instead of IFEQ
    branchIfTrue(instr.getCondition(), shortCircuitLabel);
    // Stack: empty (both paths - continue if false, branch if true)

    // 3. Full evaluation path: left was false, evaluate right operand
    processConditionEvaluation(instr.getRightEvaluationInstructions());
    // Stack: empty, variables: right_operand (Boolean)

    // 4. Call _or() operator
    processConditionEvaluation(instr.getResultComputationInstructions());
    // Stack: empty, variables: logical_result (Boolean from _or())

    // 5. Copy logical_result to instruction result variable
    copyResultVariable(instr.getLogicalResult(), instr.getResult());
    // Stack: empty

    // 6. Jump over short-circuit path
    jumpTo(endLabel);
    // Stack: empty at end label

    // 7. Short-circuit label: left was true, use left as result
    placeLabel(shortCircuitLabel);
    // Stack: empty (from IFNE branch)

    // 8. Copy left_operand to instruction result (short-circuit: true OR anything = true)
    copyResultVariable(instr.getLeftOperand(), instr.getResult());
    // Stack: empty

    // 9. End label: both paths converge with empty stack
    placeLabel(endLabel);
    // Stack: empty
    // instruction result now contains final Boolean (either left_operand or logical_result)
  }
}
