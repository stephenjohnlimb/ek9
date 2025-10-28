package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
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
                     final ClassWriter classWriter,
                     final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter, context);
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
    // Query context: are we inside a loop?
    final Label endLabel;
    final boolean shouldPlaceEndLabel;

    if (context.isInsideLoop()) {
      // Inside loop: use loop's continue label (next iteration)
      endLabel = context.getLoopContinueLabel()
          .orElseThrow(() -> new IllegalStateException("Loop context missing continue label"));
      shouldPlaceEndLabel = false;  // Loop owns this label, don't place it here
    } else {
      // Outside loop: create local end label
      endLabel = createControlFlowLabel("if_end", instr.getScopeId());
      shouldPlaceEndLabel = true;
    }

    Label nextConditionLabel;
    var conditionChain = instr.getConditionChain();

    // Process each condition case (if, else if, else if, ...)
    for (ConditionCaseDetails conditionCase : conditionChain) {
      // Create label for next condition or else block using branch scope ID
      // Each branch has unique scope ID from IR, ensuring unique labels
      nextConditionLabel = createControlFlowLabel(
          "if_next",
          conditionCase.caseScopeId());

      // Process this condition case WITHOUT placing the label
      // This will:
      // 1. Evaluate condition evaluation instructions
      // 2. Load primitive condition and branch to nextConditionLabel if false
      // 3. Execute body evaluation if true (including SCOPE_ENTER/EXIT)
      // 4. Jump to endLabel after body
      processConditionCaseWithoutLabelPlacement(conditionCase, nextConditionLabel, endLabel, null);
      // Stack: empty at endLabel (from jump)

      // Place the nextConditionLabel for the next iteration (or else block)
      // This is where execution continues if the condition was false
      placeLabel(nextConditionLabel);
      // Stack: empty (from branch)
    }

    // At this point, we're at the label where all conditions were false
    // The default case (else block) will execute here

    // Process else block if present (default case)
    if (instr.hasDefaultCase()) {
      // Process default body evaluation
      processBodyEvaluation(instr.getDefaultBodyEvaluation());
      // Stack: empty

      // No need to jump to end as we fall through
    }

    // End label: all paths converge here
    // Only place label if we created it locally (not owned by loop)
    if (shouldPlaceEndLabel) {
      placeLabel(endLabel);
      // Stack: empty (guaranteed by all paths)
    }
  }
}