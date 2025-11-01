package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * Specialized generator for EK9 try/catch/finally statements.
 * Handles CONTROL_FLOW_CHAIN with chain_type: "TRY_CATCH_FINALLY".
 * <p>
 * JVM Bytecode Pattern:
 * </p>
 * <pre>
 * try_start:
 *   [try block instructions]
 *   goto after_handlers            ; Normal exit
 * try_end:
 * catch_start_1:
 *   [catch block 1 instructions]
 *   goto after_handlers
 * catch_end_1:
 * catch_start_2:
 *   [catch block 2 instructions]
 *   goto after_handlers
 * catch_end_2:
 * after_handlers:
 *   [finally block if present]
 *
 * Exception Table:
 *   from    to      target        type
 *   try_start try_end catch_start_1 ExceptionType1
 *   try_start try_end catch_start_2 ExceptionType2
 * </pre>
 * <p>
 * Stack Frame Invariant: Stack is empty before try, after each handler,
 * and after the entire statement.
 * </p>
 */
final class TryCatchAsmGenerator extends AbstractControlFlowAsmGenerator {

  TryCatchAsmGenerator(final ConstructTargetTuple constructTargetTuple, final OutputVisitor outputVisitor,
                       final ClassWriter classWriter, final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter, context);
  }

  /**
   * Generate bytecode for try/catch/finally.
   * <p>
   * Control Flow:
   * </p>
   * <ol>
   *   <li>Create labels for exception handlers</li>
   *   <li>Register exception handlers with JVM (visitTryCatchBlock)</li>
   *   <li>Execute try block</li>
   *   <li>Jump to after handlers (normal path)</li>
   *   <li>Execute each catch handler</li>
   *   <li>Execute finally block (if present)</li>
   * </ol>
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty
   * </p>
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with TRY_CATCH_FINALLY type
   */
  public void generate(final ControlFlowChainInstr instr) {
    final var tryDetails = instr.getTryBlockDetails();
    final var catchHandlers = instr.getConditionChain();  // Each "case" is a catch handler
    final var finallyBody = instr.getDefaultBodyEvaluation();  // Finally block

    // Validation: must have at least try block
    if (tryDetails == null) {
      throw new org.ek9lang.core.CompilerException(
          "TRY_CATCH_FINALLY must have tryBlockDetails");
    }

    // Validation: must have at least one catch handler for now (finally-only not yet implemented)
    if (catchHandlers.isEmpty() && finallyBody.isEmpty()) {
      throw new org.ek9lang.core.CompilerException(
          "TRY_CATCH_FINALLY must have at least one catch handler or finally block");
    }

    // 1. Create ALL labels upfront
    final var tryStartLabel = new Label();
    final var tryEndLabel = new Label();
    final var afterHandlersLabel = new Label();

    // Create handler labels (must match visitTryCatchBlock calls)
    final var handlerLabels = new java.util.ArrayList<Label>();
    for (int i = 0; i < catchHandlers.size(); i++) {
      handlerLabels.add(new Label());
    }

    // 2. Register exception handlers with JVM
    // Each catch handler needs try_start, try_end, handler_start, exception_type
    for (int i = 0; i < catchHandlers.size(); i++) {
      final var catchHandler = catchHandlers.get(i);

      // Extract exception type from IR and convert to JVM internal format
      // IR: "org.ek9.lang::Exception" -> JVM: "org/ek9/lang/Exception"
      final var ek9ExceptionType = catchHandler.exceptionType();
      final var jvmExceptionType = convertToJvmName(ek9ExceptionType);

      getCurrentMethodVisitor().visitTryCatchBlock(tryStartLabel, tryEndLabel, handlerLabels.get(i), jvmExceptionType);
    }

    // 3. Execute try block
    placeLabel(tryStartLabel);
    // Stack: empty

    // Process try body (includes SCOPE_ENTER/EXIT from IR)
    processBodyEvaluation(tryDetails.tryBodyEvaluation());
    // Stack: empty

    placeLabel(tryEndLabel);

    // 4. Normal path: jump to after handlers
    jumpTo(afterHandlersLabel);
    // Stack: empty

    // 5. Execute each catch handler
    for (int i = 0; i < catchHandlers.size(); i++) {
      final var catchHandler = catchHandlers.get(i);
      final var handlerLabel = handlerLabels.get(i);

      // Place handler label (matches visitTryCatchBlock registration)
      placeLabel(handlerLabel);

      // Stack: exception object (automatically pushed by JVM)
      // Store exception in local variable
      final var exceptionVar = catchHandler.exceptionVariable();
      if (exceptionVar != null) {
        final var varIndex = getVariableIndex(exceptionVar);
        getCurrentMethodVisitor().visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, varIndex);
      } else {
        // No exception variable - just pop the exception from stack
        getCurrentMethodVisitor().visitInsn(org.objectweb.asm.Opcodes.POP);
      }
      // Stack: empty

      // Process catch body (includes SCOPE_ENTER/EXIT from IR)
      processBodyEvaluation(catchHandler.bodyEvaluation());
      // Stack: empty

      // Jump to after handlers
      jumpTo(afterHandlersLabel);
    }

    // 6. Place after handlers label
    placeLabel(afterHandlersLabel);
    // Stack: empty

    // 7. Execute finally block if present
    if (!finallyBody.isEmpty()) {
      processBodyEvaluation(finallyBody);
      // Stack: empty
    }

    // Stack: empty (invariant maintained)
  }
}
