package org.ek9lang.compiler.backend.jvm;

import java.util.function.Consumer;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ThrowInstr;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Specialized ASM generator for ThrowInstr processing.
 * Handles THROW operations - generates ATHROW bytecode instruction to throw exceptions.
 * <p>
 * JVM ATHROW Behavior:
 * - Stack before: [objectref] (exception object on stack)
 * - Stack after: [] (empty - execution transfers to exception handler or exits method)
 * - Opcode: ATHROW (191 / 0xBF)
 * - Throws: NullPointerException if objectref is null
 * </p>
 * <p>
 * Pattern:
 * 1. Exception object created via constructor call (already on stack or in variable)
 * 2. RETAIN and SCOPE_REGISTER performed by IR (ARC semantics)
 * 3. THROW loads variable and executes ATHROW
 * 4. JVM searches for matching exception handler in current method
 * 5. If no handler found, method exits and search continues up call stack
 * </p>
 */
final class ThrowInstrAsmGenerator extends AbstractAsmGenerator
    implements Consumer<ThrowInstr> {

  ThrowInstrAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                         final OutputVisitor outputVisitor,
                         final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate JVM ATHROW instruction for a throw statement.
   * Loads the exception variable onto the stack and throws it.
   */
  @Override
  public void accept(final ThrowInstr throwInstr) {
    AssertValue.checkNotNull("ThrowInstr cannot be null", throwInstr);

    // Generate debug info if available (important for stack traces)
    throwInstr.getDebugInfo().ifPresent(this::generateDebugInfo);

    final var exceptionVariable = throwInstr.getExceptionVariable();
    if (exceptionVariable == null || exceptionVariable.isEmpty()) {
      throw new CompilerException("THROW instruction requires exception variable");
    }

    // Load the exception object onto the stack
    // The exception should already be a valid Exception object from previous instructions
    generateLoadVariable(exceptionVariable);

    // Generate ATHROW instruction
    // This transfers control to the exception handler or exits the method
    getCurrentMethodVisitor().visitInsn(Opcodes.ATHROW);
  }
}
