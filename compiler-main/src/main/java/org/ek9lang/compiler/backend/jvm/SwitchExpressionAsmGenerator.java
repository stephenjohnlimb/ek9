package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * Specialized generator for EK9 switch expression form.
 * Handles CONTROL_FLOW_CHAIN with chain_type: "SWITCH_EXPRESSION".
 * <p>
 * EK9 Switch Expression Pattern:
 * </p>
 * <pre>
 * result &lt;- switch value
 *   &lt;- rtn as String: "Unknown"
 *   case 1
 *     rtn: "One"
 *   case 2
 *     rtn: "Two"
 *   default
 *     rtn: "Other"
 * </pre>
 * <p>
 * Key Differences from Switch Statement:
 * </p>
 * <ul>
 *   <li>Has return variable declared with returningParam ({@code <- rtn as Type: defaultValue})</li>
 *   <li>Each case body stores result into return variable</li>
 *   <li>Default case also stores into return variable</li>
 *   <li>Return variable transferred to result variable after switch completes</li>
 * </ul>
 * <p>
 * IR Structure:
 * </p>
 * <pre>
 * CONTROL_FLOW_CHAIN [chain_type: "SWITCH_EXPRESSION"]
 *   return_variable: rtn
 *   return_variable_type: org.ek9.lang::String
 *   return_variable_setup:
 *     [REFERENCE rtn, LOAD_LITERAL "Unknown", RETAIN, SCOPE_REGISTER, STORE rtn, RETAIN rtn]
 *   evaluation_variable: _temp3 (switch value)
 *   condition_chain:
 *     [case 1: LITERAL check]
 *       condition_evaluation: [load value, compare with 1]
 *       body_evaluation: [SCOPE_ENTER, LOAD_LITERAL "One", RELEASE rtn, STORE rtn, RETAIN rtn, SCOPE_EXIT]
 *       body_result: rtn
 *     [case 2: LITERAL check]
 *       ...
 *   default_body_evaluation:
 *     [SCOPE_ENTER, LOAD_LITERAL "Other", RELEASE rtn, STORE rtn, RETAIN rtn, SCOPE_EXIT]
 *   default_result: rtn
 *   result: rtn (overall result variable)
 * </pre>
 * <p>
 * Bytecode Pattern (Sequential Evaluation):
 * </p>
 * <pre>
 * ; Return variable setup (initialize with default value)
 * [return_variable_setup instructions]
 *
 * ; Evaluation variable setup (switch expression)
 * [evaluation variable already loaded by AssignmentExprInstrGenerator]
 *
 * ; Case 1
 * [condition evaluation]
 * iload primitive_condition
 * ifeq case_2                    ; Branch if false
 * [case 1 body - stores into rtn]
 * goto end                       ; Exit switch
 *
 * case_2:
 * [condition evaluation]
 * iload primitive_condition
 * ifeq default_label             ; Branch if false
 * [case 2 body - stores into rtn]
 * goto end
 *
 * default_label:
 * [default body - stores into rtn]
 *                                ; Fall through to end
 *
 * end:
 * ; rtn now holds the result value
 * ; Ownership transfer happens in AssignmentExprInstrGenerator:
 * ;   STORE _temp1, rtn
 * ;   RELEASE rtn
 * ;   RETAIN _temp1
 * ;   SCOPE_REGISTER _temp1
 * </pre>
 * <p>
 * Stack Frame Invariant: All paths arrive at 'end' label with empty stack.
 * Result is in the return variable (rtn), ready for ownership transfer.
 * </p>
 * <p>
 * Design Note: Like switch statement, uses sequential evaluation (if/else-if pattern)
 * rather than JVM tableswitch/lookupswitch for rich operator semantics and type flexibility.
 * </p>
 */
final class SwitchExpressionAsmGenerator extends AbstractControlFlowAsmGenerator {

  SwitchExpressionAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                               final OutputVisitor outputVisitor,
                               final ClassWriter classWriter,
                               final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter, context);
  }

  /**
   * Generate bytecode for switch expression.
   * <p>
   * Control Flow:
   * </p>
   * <ol>
   *   <li>Return variable setup already processed (happens before CONTROL_FLOW_CHAIN)</li>
   *   <li>For each case: evaluate condition, branch if false, execute body, jump to end</li>
   *   <li>Process default case (updates return variable)</li>
   *   <li>Place end label where all paths converge</li>
   * </ol>
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (result in return variable)
   * </p>
   * <p>
   * NOTE: Return variable setup (rtn initialization with default value) happens in
   * IR generation before this CONTROL_FLOW_CHAIN instruction. Those instructions are
   * already processed by the time we reach this generator. See AssignmentExprInstrGenerator
   * where processSwitchExpression() calls the switch statement generator.
   * </p>
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with SWITCH_EXPRESSION type
   */
  public void generate(final ControlFlowChainInstr instr) {
    // Create end label where all paths converge
    final var endLabel = createControlFlowLabel("switch_expr_end", instr.getScopeId());

    Label nextCaseLabel;
    var conditionChain = instr.getConditionChain();

    // Process each case condition (case 1, case 2, case 3, ...)
    for (ConditionCaseDetails conditionCase : conditionChain) {
      // Create label for next case using unique branch scope ID from IR
      nextCaseLabel = createControlFlowLabel(
          "switch_expr_next",
          conditionCase.caseScopeId());

      // Process this case WITHOUT placing the label
      // This will:
      // 1. Evaluate condition evaluation instructions
      // 2. Load primitive condition and branch to nextCaseLabel if false
      // 3. Execute body evaluation if true (stores result into rtn)
      // 4. Jump to endLabel after body
      // NOTE: For switch expression, body_result is the return variable (rtn),
      //       so no copying needed - result is already in correct variable
      processConditionCaseWithoutLabelPlacement(conditionCase, nextCaseLabel, endLabel, null);
      // Stack: empty at endLabel (from jump)

      // Place the nextCaseLabel for the next iteration (or default case)
      // This is where execution continues if the condition was false
      placeLabel(nextCaseLabel);
      // Stack: empty (from branch)
    }

    // At this point, we're at the label where all case conditions were false
    // The default case will execute here

    // Process default case (stores result into rtn)
    if (instr.hasDefaultCase()) {
      // Process default body evaluation
      processBodyEvaluation(instr.getDefaultBodyEvaluation());
      // Stack: empty

      // No need to jump to end as we fall through
      // NOTE: For switch expression, default_result is the return variable (rtn),
      //       so no copying needed - result is already in correct variable
    }

    // End label: all paths converge here
    placeLabel(endLabel);
    // Stack: empty (guaranteed by all paths)
    // Result is in return variable (instr.getReturnVariable())
    // Ownership transfer happens in AssignmentExprInstrGenerator:
    //   STORE target, rtn
    //   RELEASE rtn
    // Then VariableMemoryManagement adds:
    //   RETAIN target
    //   SCOPE_REGISTER target
  }
}
