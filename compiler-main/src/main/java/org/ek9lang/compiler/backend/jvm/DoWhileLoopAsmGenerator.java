package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.objectweb.asm.ClassWriter;

/**
 * Specialized generator for EK9 do-while loops.
 * Handles CONTROL_FLOW_CHAIN with chain_type: "DO_WHILE_LOOP".
 * <p>
 * JVM Bytecode Pattern (different from while loop):
 * </p>
 * <pre>
 * loop_start:
 *   [body instructions]         ; Execute body FIRST
 *   [condition evaluation]      ; Then evaluate condition
 *   iload primitive_condition
 *   ifne loop_start             ; If TRUE (non-zero), jump BACK
 * loop_end:                     ; Fall through if false
 * </pre>
 * <p>
 * Key Differences from While Loop:
 * </p>
 * <ul>
 *   <li>Body executes BEFORE condition (guaranteed at least once)</li>
 *   <li>Uses IFNE (jump if NON-ZERO/true) instead of IFEQ</li>
 *   <li>No need for forward branch before body</li>
 *   <li>Loop always executes at least once</li>
 * </ul>
 * <p>
 * Stack Frame Invariant: Stack is empty before loop, after body,
 * after condition, and after loop exit.
 * </p>
 */
final class DoWhileLoopAsmGenerator extends AbstractControlFlowAsmGenerator {

  DoWhileLoopAsmGenerator(final ConstructTargetTuple constructTargetTuple, final OutputVisitor outputVisitor,
                          final ClassWriter classWriter, final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter, context);
  }

  /**
   * Generate bytecode for do-while loop.
   * <p>
   * Control Flow:
   * </p>
   * <ol>
   *   <li>Place loop start label</li>
   *   <li>Execute body (leaves stack empty)</li>
   *   <li>Evaluate condition (leaves stack empty)</li>
   *   <li>Branch BACK to loop start if condition is TRUE</li>
   *   <li>Fall through to loop end if condition is FALSE</li>
   * </ol>
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty
   * </p>
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with DO_WHILE_LOOP type
   */
  public void generate(final ControlFlowChainInstr instr) {
    // 1. Create label for loop control flow
    final var loopStartLabel = createControlFlowLabel("do_while_start", instr.getScopeId());

    // 2. Get the single condition case
    final var conditionCase = instr.getConditionChain().getFirst();

    // 3. Place loop start label
    placeLabel(loopStartLabel);
    // Stack: empty

    // 4. Process body FIRST (key do-while characteristic)
    processBodyEvaluation(conditionCase.bodyEvaluation());
    // Stack: empty

    // 5. Process condition evaluation (AFTER body)
    processConditionEvaluation(conditionCase.conditionEvaluation());
    // Stack: empty (condition result in local variable)

    // 6. Branch BACK to start if condition is TRUE
    // NOTE: Use branchIfTrue() instead of branchIfFalse()
    // This is the key difference from while loops!
    branchIfTrue(conditionCase.primitiveCondition(), loopStartLabel);
    // Stack: empty (both paths - fall through or branch back)

    // 7. Fall through to loop end (no explicit label needed)
    // The loop naturally exits when condition is false
    // Stack: empty
  }
}
