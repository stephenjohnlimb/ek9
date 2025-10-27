package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * Specialized generator for EK9 switch statements.
 * Handles CONTROL_FLOW_CHAIN with chain_type: "SWITCH".
 * <p>
 * EK9 Switch Semantics:
 * - Sequential condition evaluation (if/else-if pattern)
 * - Supports rich operators: equality, contains, matches, comparison
 * - Works with Integer, String, Float, Character types
 * - Multiple case literals per case
 * - Optional default clause
 * </p>
 * <p>
 * Bytecode Pattern (Sequential Evaluation):
 * </p>
 * <pre>
 * ; Evaluation variable setup (switch expression)
 * [evaluation variable instructions]
 *
 * ; Case 1
 * [condition evaluation]
 * iload primitive_condition
 * ifeq case_2                    ; Branch if false
 * [case 1 body]
 * goto end                       ; Exit switch
 *
 * case_2:
 * [condition evaluation]
 * iload primitive_condition
 * ifeq default_label             ; Branch if false
 * [case 2 body]
 * goto end
 *
 * default_label:
 * [default body]                 ; Fall through to end
 *
 * end:
 * ; All paths converge here
 * </pre>
 * <p>
 * Stack Frame Invariant: All paths arrive at 'end' label with empty stack.
 * Statement form has no result variable (unlike expression form which would).
 * </p>
 * <p>
 * Design Note: EK9 switch uses sequential evaluation rather than JVM tableswitch/lookupswitch
 * because it supports rich operator semantics (contains, matches) and multiple types (String,
 * Float) that don't map to JVM switch instructions. This provides maximum flexibility while
 * maintaining correct semantics.
 * </p>
 */
final class SwitchAsmGenerator extends AbstractControlFlowAsmGenerator {

  SwitchAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                     final OutputVisitor outputVisitor,
                     final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate bytecode for switch statement.
   * <p>
   * Control Flow:
   * </p>
   * <ol>
   *   <li>Process evaluation variable setup (switch expression)</li>
   *   <li>For each case: evaluate condition, branch if false, execute body, jump to end</li>
   *   <li>Process default case (if present)</li>
   *   <li>Place end label where all paths converge</li>
   * </ol>
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty
   * </p>
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with SWITCH type
   */
  public void generate(final ControlFlowChainInstr instr) {
    // Create end label where all paths converge
    final var endLabel = createControlFlowLabel("switch_end", instr.getScopeId());

    Label nextCaseLabel;
    var conditionChain = instr.getConditionChain();

    // Process each case condition (case 1, case 2, case 3, ...)
    for (ConditionCaseDetails conditionCase : conditionChain) {
      // Create label for next case using unique branch scope ID from IR
      nextCaseLabel = createControlFlowLabel(
          "switch_next",
          conditionCase.caseScopeId());

      // Process this case WITHOUT placing the label
      // This will:
      // 1. Evaluate condition evaluation instructions
      // 2. Load primitive condition and branch to nextCaseLabel if false
      // 3. Execute body evaluation if true (including SCOPE_ENTER/EXIT)
      // 4. Jump to endLabel after body
      processConditionCaseWithoutLabelPlacement(conditionCase, nextCaseLabel, endLabel, null);
      // Stack: empty at endLabel (from jump)

      // Place the nextCaseLabel for the next iteration (or default case)
      // This is where execution continues if the condition was false
      placeLabel(nextCaseLabel);
      // Stack: empty (from branch)
    }

    // At this point, we're at the label where all case conditions were false
    // The default case will execute here

    // Process default case if present
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
