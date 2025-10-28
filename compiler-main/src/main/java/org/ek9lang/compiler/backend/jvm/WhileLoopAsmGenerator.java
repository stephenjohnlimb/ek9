package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.objectweb.asm.ClassWriter;

/**
 * Specialized generator for EK9 while loops.
 * Handles CONTROL_FLOW_CHAIN with chain_type: "WHILE_LOOP".
 * <p>
 * JVM Bytecode Pattern:
 * </p>
 * <pre>
 * loop_start:
 *   [condition evaluation]
 *   iload primitive_condition
 *   ifeq loop_end              ; Exit if false
 *   [body instructions]
 *   goto loop_start            ; Iterate again
 * loop_end:
 * </pre>
 * <p>
 * Stack Frame Invariant: Stack is empty before loop, after condition,
 * after body, and after loop exit.
 * </p>
 */
final class WhileLoopAsmGenerator extends AbstractControlFlowAsmGenerator {

  WhileLoopAsmGenerator(final ConstructTargetTuple constructTargetTuple, final OutputVisitor outputVisitor,
                        final ClassWriter classWriter, final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter, context);
  }

  /**
   * Generate bytecode for while loop.
   * <p>
   * Control Flow:
   * </p>
   * <ol>
   *   <li>Place loop start label</li>
   *   <li>Evaluate condition (leaves stack empty)</li>
   *   <li>Branch to loop end if condition is false</li>
   *   <li>Execute body (leaves stack empty)</li>
   *   <li>Jump back to loop start</li>
   *   <li>Place loop end label</li>
   * </ol>
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty
   * </p>
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with WHILE_LOOP type
   */
  public void generate(final ControlFlowChainInstr instr) {
    // 1. Create labels for loop control flow
    final var loopStartLabel = createControlFlowLabel("while_start", instr.getScopeId());
    final var loopEndLabel = createControlFlowLabel("while_end", instr.getScopeId());

    // 2. Get the single condition case (while loops have exactly one)
    final var conditionCase = instr.getConditionChain().getFirst();

    // 3. Place loop start label
    placeLabel(loopStartLabel);
    // Stack: empty

    // 4. Process condition evaluation (includes SCOPE_ENTER/EXIT from IR)
    processConditionEvaluation(conditionCase.conditionEvaluation());
    // Stack: empty (condition result in local variable)

    // 5. Branch to end if condition is false
    branchIfFalse(conditionCase.primitiveCondition(), loopEndLabel);
    // Stack: empty (both paths - continue or branch)

    // 6. Process body (includes SCOPE_ENTER/EXIT from IR)
    processBodyEvaluation(conditionCase.bodyEvaluation());
    // Stack: empty

    // 7. Jump back to start for next iteration
    jumpTo(loopStartLabel);
    // Stack: irrelevant (control transferred)

    // 8. Place loop end label (reached when condition is false)
    placeLabel(loopEndLabel);
    // Stack: empty (from branch in step 5)
  }
}
