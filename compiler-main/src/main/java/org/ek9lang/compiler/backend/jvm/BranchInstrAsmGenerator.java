package org.ek9lang.compiler.backend.jvm;

import java.util.function.Consumer;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 * Specialized ASM generator for BranchInstr processing.
 * Handles RETURN, BRANCH, BRANCH_TRUE, BRANCH_FALSE, ASSERT operations
 * using the actual BranchInstr methods (no string parsing).
 */
public final class BranchInstrAsmGenerator extends AbstractAsmGenerator
    implements Consumer<BranchInstr> {

  private boolean isConstructor = false;

  public BranchInstrAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                 final OutputVisitor outputVisitor,
                                 final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Set whether we're currently processing a constructor.
   * Must be called before processing constructor instructions.
   */
  public void setConstructorMode(final boolean isConstructor) {
    this.isConstructor = isConstructor;
  }

  /**
   * Generate JVM bytecode for a branch instruction.
   * Uses BranchInstr opcode to determine the specific operation.
   */
  @Override
  public void accept(final BranchInstr branchInstr) {
    AssertValue.checkNotNull("BranchInstr cannot be null", branchInstr);

    // Generate debug info if available
    branchInstr.getDebugInfo().ifPresent(this::generateDebugInfo);

    // Generate different bytecode based on branch operation type
    switch (branchInstr.getOpcode()) {
      case RETURN -> generateReturn(branchInstr);
      case BRANCH -> generateBranch(branchInstr);
      case BRANCH_TRUE -> generateBranchTrue(branchInstr);
      case BRANCH_FALSE -> generateBranchFalse(branchInstr);
      case ASSERT -> generateAssert(branchInstr);
      default -> throw new CompilerException("Unhandled branch opcode: " + branchInstr.getOpcode());
    }
  }

  /**
   * Generate RETURN instruction.
   * Uses proper variable map instead of hashcode hack.
   */
  private void generateReturn(final BranchInstr branchInstr) {
    final var mv = getCurrentMethodVisitor();

    // JVM constructors ALWAYS return void, regardless of EK9 IR semantics
    if (isConstructor) {
      mv.visitInsn(Opcodes.RETURN);
      return;
    }

    // Regular methods: check if we need to return a value
    final var returnValue = branchInstr.getReturnValue();
    if (returnValue != null && !returnValue.isEmpty()) {
      // Load return value onto stack
      if ("this".equals(returnValue)) {
        // Load 'this' (always slot 0)
        mv.visitVarInsn(Opcodes.ALOAD, 0);
      } else {
        // Use proper variable map instead of hashcode hack
        final int varIndex = getVariableIndex(returnValue);
        mv.visitVarInsn(Opcodes.ALOAD, varIndex);
      }
      mv.visitInsn(Opcodes.ARETURN); // Return object reference
    } else {
      mv.visitInsn(Opcodes.RETURN); // Return void
    }
  }

  /**
   * Generate unconditional BRANCH (GOTO).
   * Target label extracted from branch instruction.
   */
  private void generateBranch(final BranchInstr branchInstr) {
    final var targetLabel = branchInstr.getTargetLabel();
    if (targetLabel == null || targetLabel.isEmpty()) {
      throw new CompilerException("BRANCH instruction requires target label");
    }

    // Create or reuse JVM label
    final Label jvmLabel = getOrCreateLabel(targetLabel);
    getCurrentMethodVisitor().visitJumpInsn(Opcodes.GOTO, jvmLabel);
  }

  /**
   * Generate conditional BRANCH_TRUE (IFNE - if not equal to zero).
   * Branches if condition is true (non-zero).
   */
  private void generateBranchTrue(final BranchInstr branchInstr) {
    final var condition = branchInstr.getCondition();
    final var targetLabel = branchInstr.getTargetLabel();

    if (condition == null || condition.isEmpty()) {
      throw new CompilerException("BRANCH_TRUE instruction requires condition variable");
    }
    if (targetLabel == null || targetLabel.isEmpty()) {
      throw new CompilerException("BRANCH_TRUE instruction requires target label");
    }

    // Load condition variable (should be Boolean converted to int)
    final int varIndex = getVariableIndex(condition);
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ILOAD, varIndex);

    // Branch if true (non-zero)
    final Label jvmLabel = getOrCreateLabel(targetLabel);
    getCurrentMethodVisitor().visitJumpInsn(Opcodes.IFNE, jvmLabel);
  }

  /**
   * Generate conditional BRANCH_FALSE (IFEQ - if equal to zero).
   * Branches if condition is false (zero).
   */
  private void generateBranchFalse(final BranchInstr branchInstr) {
    final var condition = branchInstr.getCondition();
    final var targetLabel = branchInstr.getTargetLabel();

    if (condition == null || condition.isEmpty()) {
      throw new CompilerException("BRANCH_FALSE instruction requires condition variable");
    }
    if (targetLabel == null || targetLabel.isEmpty()) {
      throw new CompilerException("BRANCH_FALSE instruction requires target label");
    }

    // Load condition variable (should be Boolean converted to int)
    final int varIndex = getVariableIndex(condition);
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ILOAD, varIndex);

    // Branch if false (zero)
    final Label jvmLabel = getOrCreateLabel(targetLabel);
    getCurrentMethodVisitor().visitJumpInsn(Opcodes.IFEQ, jvmLabel);
  }

  /**
   * Generate ASSERT instruction.
   * Evaluates condition and throws AssertionError if false.
   */
  private void generateAssert(final BranchInstr branchInstr) {
    final var condition = branchInstr.getCondition();
    if (condition == null || condition.isEmpty()) {
      throw new CompilerException("ASSERT instruction requires condition variable");
    }

    final var mv = getCurrentMethodVisitor();

    // Load condition variable
    final int varIndex = getVariableIndex(condition);
    mv.visitVarInsn(Opcodes.ILOAD, varIndex);

    // If condition is true (non-zero), skip assertion failure
    final Label assertionPassed = new Label();
    mv.visitJumpInsn(Opcodes.IFNE, assertionPassed);

    // Condition is false - throw AssertionError
    mv.visitTypeInsn(Opcodes.NEW, "java/lang/AssertionError");
    mv.visitInsn(Opcodes.DUP);

    // Generate assertion message with source location if available
    final var message = generateAssertMessage(branchInstr);
    mv.visitLdcInsn(message);

    mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
        "java/lang/AssertionError",
        "<init>",
        "(Ljava/lang/Object;)V",
        false);
    mv.visitInsn(Opcodes.ATHROW);

    // Assertion passed - continue execution
    mv.visitLabel(assertionPassed);
  }

  /**
   * Generate assertion message with source location from debug info.
   */
  private String generateAssertMessage(final BranchInstr branchInstr) {
    if (branchInstr.getDebugInfo().isPresent()) {
      final var debugInfo = branchInstr.getDebugInfo().get();
      if (debugInfo.isValidLocation()) {
        return String.format("Assertion failed at %s:%d:%d",
            debugInfo.sourceFile(),
            debugInfo.lineNumber(),
            debugInfo.columnNumber());
      }
    }
    return "Assertion failed";
  }
}
