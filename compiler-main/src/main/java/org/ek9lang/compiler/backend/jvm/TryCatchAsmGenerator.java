package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.ek9lang.core.CompilerException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * Specialized generator for EK9 try/catch/finally statements.
 * Handles CONTROL_FLOW_CHAIN with chain_type: "TRY_CATCH_FINALLY".
 * <p>
 * JVM Bytecode Pattern for try/catch:
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
 * after_handlers:
 *   [continue execution]
 * </pre>
 * <p>
 * JVM Bytecode Pattern for try/finally (unified finally block):
 * </p>
 * <pre>
 * try_start:
 *   [try block instructions]
 * try_end:
 *   goto finally_block            ; Normal path
 * catch_start:
 *   [catch block instructions]
 *   goto finally_block            ; Catch path
 * finally_exception:
 *   astore temp_exception         ; Store exception
 *   goto finally_block            ; Exception path
 * finally_block:
 *   [finally block - executes once for all paths]
 *   aload temp_exception (if set)
 *   ifnull after_finally
 *   athrow                        ; Rethrow if exception present
 * after_finally:
 *   [continue execution]
 *
 * Exception Table:
 *   from    to      target          type
 *   try_start try_end finally_exception null (catch-all)
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
   *   <li>Create labels for exception handlers and finally blocks</li>
   *   <li>Register exception handlers with JVM (visitTryCatchBlock)</li>
   *   <li>Execute try block</li>
   *   <li>Execute finally block (normal path) if present</li>
   *   <li>Jump to after handlers</li>
   *   <li>Execute each catch handler</li>
   *   <li>Execute finally block (exception path) if present</li>
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
    final var finallyBody = instr.getFinallyBlockEvaluation();  // Finally block
    final var hasFinallyBlock = !finallyBody.isEmpty();

    // Validation: must have at least try block
    if (tryDetails == null) {
      throw new CompilerException(
          "TRY_CATCH_FINALLY must have tryBlockDetails");
    }

    // Validation: must have at least one catch handler or finally block
    if (catchHandlers.isEmpty() && !hasFinallyBlock) {
      throw new CompilerException(
          "TRY_CATCH_FINALLY must have at least one catch handler or finally block");
    }

    // 1. Create ALL labels upfront
    final var tryStartLabel = new Label();
    final var tryEndLabel = new Label();
    final var afterHandlersLabel = new Label();
    final var finallyExceptionLabel = hasFinallyBlock ? new Label() : null;
    final var finallyBlockLabel = hasFinallyBlock ? new Label() : null;
    final var catchEndLabel = !catchHandlers.isEmpty() ? new Label() : null;

    // Create handler labels (must match visitTryCatchBlock calls)
    final var handlerLabels = new java.util.ArrayList<Label>();
    for (int i = 0; i < catchHandlers.size(); i++) {
      handlerLabels.add(new Label());
    }

    // Create duplicate handler labels for exceptions FROM finally block
    // These handlers are identical but skip the jump back to finally (preventing infinite loop)
    final var finallySourceHandlerLabels = new java.util.ArrayList<Label>();
    if (hasFinallyBlock) {
      for (int i = 0; i < catchHandlers.size(); i++) {
        finallySourceHandlerLabels.add(new Label());
      }
    }

    // 2. Initialize temp exception variable to null (if finally block exists)
    // NOTE: Exception handler registration moved to after block generation (see step 8)
    final Integer tempExceptionIndex;
    if (hasFinallyBlock) {
      tempExceptionIndex = getVariableIndex("_temp_finally_exception");
      getCurrentMethodVisitor().visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
      getCurrentMethodVisitor().visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, tempExceptionIndex);
    } else {
      tempExceptionIndex = null;
    }

    // 4. Execute try block
    placeLabel(tryStartLabel);
    // Stack: empty

    // Process try body (includes SCOPE_ENTER/EXIT from IR)
    processBodyEvaluation(tryDetails.tryBodyEvaluation());
    // Stack: empty

    placeLabel(tryEndLabel);

    // 4. Normal path: jump to finally block (if present) or after handlers
    if (hasFinallyBlock) {
      // Jump to unified finally block
      jumpTo(finallyBlockLabel);
    } else {
      // No finally block - jump directly to after handlers
      jumpTo(afterHandlersLabel);
    }
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

      // Jump to finally block (if present) or after handlers
      if (hasFinallyBlock) {
        jumpTo(finallyBlockLabel);
      } else {
        jumpTo(afterHandlersLabel);
      }
    }

    // Mark end of REGULAR catch handlers (for finally exception range)
    // CRITICAL: This must be placed BEFORE duplicate handlers to prevent infinite loop
    // The catch-all handler range should cover: try block + regular catches
    // It should NOT cover duplicate handlers (which rethrow close() exceptions)
    if (catchEndLabel != null) {
      placeLabel(catchEndLabel);
    }

    // 5b. Generate DUPLICATE catch handlers for exceptions thrown FROM finally block
    // EK9 Semantics: close() happens in finally block (explicit or implicit)
    // Exceptions from finally ALWAYS propagate upward - NOT caught by current try/catch
    // These handlers simply RETHROW to let close() exceptions propagate past current scope
    // They are placed AFTER catchEndLabel so catch-all handler won't catch their rethrows
    if (hasFinallyBlock && !catchHandlers.isEmpty()) {
      for (int i = 0; i < catchHandlers.size(); i++) {
        final var handlerLabel = finallySourceHandlerLabels.get(i);

        // Place duplicate handler label
        placeLabel(handlerLabel);

        // Stack: exception object (automatically pushed by JVM)
        // Rethrow immediately - let close() exception propagate upward
        // This matches EK9 semantics: "finally exceptions are outside catch scope"
        getCurrentMethodVisitor().visitInsn(org.objectweb.asm.Opcodes.ATHROW);
        // Stack: empty (athrow never returns)
      }
    }

    // 6. Finally exception handler - store exception and jump to unified finally block
    if (hasFinallyBlock) {
      // Place finally exception label
      placeLabel(finallyExceptionLabel);
      // Stack: exception object (automatically pushed by JVM)

      // Store exception in temporary variable
      getCurrentMethodVisitor().visitVarInsn(org.objectweb.asm.Opcodes.ASTORE, tempExceptionIndex);
      // Stack: empty

      // Jump to unified finally block
      jumpTo(finallyBlockLabel);
      // Stack: empty

      // 7. Unified finally block - executes exactly once for all paths
      placeLabel(finallyBlockLabel);
      // Stack: empty

      // Execute finally block (only once!)
      processBodyEvaluation(finallyBody);
      // Stack: empty

      // Check if exception needs to be rethrown
      getCurrentMethodVisitor().visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, tempExceptionIndex);
      // Stack: exception object (or null if no exception)

      // If exception is null, continue to after_handlers
      getCurrentMethodVisitor().visitJumpInsn(org.objectweb.asm.Opcodes.IFNULL, afterHandlersLabel);
      // Stack: empty (exception popped by IFNULL)

      // Exception is not null - reload and rethrow
      getCurrentMethodVisitor().visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, tempExceptionIndex);
      // Stack: exception object
      getCurrentMethodVisitor().visitInsn(org.objectweb.asm.Opcodes.ATHROW);
      // Stack: empty (athrow never returns)
    }

    // 8. Register exception handlers with JVM
    // CRITICAL: Handlers registered AFTER all blocks generated to ensure correct order for nested try blocks.
    // For nested try blocks, INNER handlers are registered before OUTER handlers.
    // JVM searches exception table top-to-bottom, first match wins.

    // 8a. Register specific exception handlers FIRST
    // These must come before catch-all to ensure catches execute before finally
    for (int i = 0; i < catchHandlers.size(); i++) {
      final var catchHandler = catchHandlers.get(i);

      // Extract exception type from IR and convert to JVM internal format
      // IR: "org.ek9.lang::Exception" -> JVM: "org/ek9/lang/Exception"
      final var ek9ExceptionType = catchHandler.exceptionType();
      final var jvmExceptionType = convertToJvmName(ek9ExceptionType);

      // Cover try block
      getCurrentMethodVisitor().visitTryCatchBlock(tryStartLabel, tryEndLabel, handlerLabels.get(i), jvmExceptionType);
    }

    // 8b. Register handlers for exceptions thrown FROM finally block (e.g., resource close() calls)
    // This is critical for try-with-resources: close() exceptions must be catchable
    // Use DUPLICATE handlers that jump directly to afterHandlersLabel (not back to finally)
    if (hasFinallyBlock && !catchHandlers.isEmpty()) {
      for (int i = 0; i < catchHandlers.size(); i++) {
        final var catchHandler = catchHandlers.get(i);
        final var ek9ExceptionType = catchHandler.exceptionType();
        final var jvmExceptionType = convertToJvmName(ek9ExceptionType);

        // Cover finally block execution range using DUPLICATE handler labels
        // This prevents infinite loop: close() → duplicate handler → afterHandlersLabel (no finally re-entry)
        getCurrentMethodVisitor().visitTryCatchBlock(finallyBlockLabel, afterHandlersLabel, finallySourceHandlerLabels.get(i), jvmExceptionType);
      }
    }

    // 8c. Register finally exception handler (catch-all) AFTER specific handlers
    if (hasFinallyBlock) {
      // For try/finally: catch-all covers try block
      // For try/catch/finally: catch-all covers try block AND catch handlers
      final var finallyEndRange = catchEndLabel != null ? catchEndLabel : tryEndLabel;
      getCurrentMethodVisitor().visitTryCatchBlock(tryStartLabel, finallyEndRange, finallyExceptionLabel, null);
    }

    // 9. Place after handlers label
    placeLabel(afterHandlersLabel);
    // Stack: empty

    // Stack: empty (invariant maintained)
  }
}
