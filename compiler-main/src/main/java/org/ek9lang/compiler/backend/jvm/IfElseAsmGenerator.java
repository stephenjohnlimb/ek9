package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * Specialized generator for EK9 if/else statements.
 * Handles CONTROL_FLOW_CHAIN with chain_type: "IF_ELSE".
 * <p>
 * Supports:
 * - Simple if (no else)
 * - If with else
 * - If/else if chains
 * - If/else if/else chains
 * </p>
 * <p>
 * Stack Frame Invariant: All paths arrive at 'end' label with empty stack.
 * Statement form has no result variable (unlike expression form which would).
 * </p>
 */
final class IfElseAsmGenerator extends AbstractControlFlowAsmGenerator {

  IfElseAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                     final OutputVisitor outputVisitor,
                     final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate bytecode for if/else statement.
   * <p>
   * Bytecode Pattern:
   * </p>
   * <pre>
   * ; Evaluate first condition
   * [condition evaluation instructions]
   * load primitive_condition
   * ifeq next_or_else_label    ; Branch if false
   * [if body]
   * goto end_label
   *
   * next_or_else_label:
   * ; Either next condition or else block or fall through
   *
   * end_label:
   * ; All paths converge here
   * </pre>
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with IF_ELSE type
   */
  public void generate(final ControlFlowChainInstr instr) {
    // Create end label where all paths converge
    final var endLabel = createControlFlowLabel("if_end", instr.getScopeId());

    // Track if we've executed any true branch (for optimization later)
    var isFirstCondition = true;
    Label nextConditionLabel = null;

    // Process each condition case (if, else if, else if, ...)
    for (var conditionCase : instr.getConditionChain()) {
      // Place label for previous false branch to jump to
      if (!isFirstCondition && nextConditionLabel != null) {
        placeLabel(nextConditionLabel);
        // Stack: empty (from previous branch)
      }

      // Create label for next condition or else block
      nextConditionLabel = createControlFlowLabel(
          "if_next_" + conditionCase.caseScopeId(),
          conditionCase.caseScopeId());

      // Process this condition case
      // This will:
      // 1. Evaluate condition evaluation instructions
      // 2. Load primitive condition and branch to nextConditionLabel if false
      // 3. Execute body evaluation if true
      // 4. Jump to endLabel after body
      processConditionCase(conditionCase, nextConditionLabel, endLabel, null);
      // Stack: empty (body jumps to end, or we branched to next)

      isFirstCondition = false;
    }

    // Handle the final false case
    if (nextConditionLabel != null) {
      placeLabel(nextConditionLabel);
      // Stack: empty
    }

    // Process else block if present (default case)
    if (instr.hasDefaultCase()) {
      // Process default body evaluation
      processBodyEvaluation(instr.getDefaultBodyEvaluation());
      // Stack: empty

      // No need to jump to end as we fall through
    }

    // End label: all paths converge here
    placeLabel(endLabel);
    // Stack: empty (guaranteed by all paths)
  }
}