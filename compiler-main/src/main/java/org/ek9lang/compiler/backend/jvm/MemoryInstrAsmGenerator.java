package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.core.AssertValue;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Specialized ASM generator for MemoryInstr processing.
 * Handles LOAD, STORE, REFERENCE, RETAIN, RELEASE operations
 * using the actual MemoryInstr methods (no string parsing).
 */
public final class MemoryInstrAsmGenerator extends AbstractAsmGenerator {

  public MemoryInstrAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                 final OutputVisitor outputVisitor,
                                 final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate JVM bytecode for a memory operation instruction.
   * Uses MemoryInstr opcode to determine the specific operation.
   */
  public void generateMemoryOperation(final MemoryInstr memoryInstr) {
    AssertValue.checkNotNull("MemoryInstr cannot be null", memoryInstr);

    // Generate debug info if available
    memoryInstr.getDebugInfo().ifPresent(this::generateDebugInfo);

    // Generate different bytecode based on memory operation type
    switch (memoryInstr.getOpcode()) {
      case LOAD -> generateLoad(memoryInstr);
      case STORE -> generateStore(memoryInstr);
      case REFERENCE -> generateReference(memoryInstr);
      case RETAIN -> generateRetain(memoryInstr);
      case RELEASE -> generateRelease(memoryInstr);
      case IS_NULL -> generateIsNull(memoryInstr);
      case FUNCTION_INSTANCE -> generateFunctionInstance(memoryInstr);
      default -> throw new IllegalArgumentException("Unsupported memory opcode: " + memoryInstr.getOpcode());
    }
  }

  /**
   * Generate LOAD instruction: load value from variable to stack.
   * For JVM backend, LOAD operations just put values on stack for consumption.
   * Format: LOAD dest = source_location (dest is typically a temp that gets consumed)
   */
  private void generateLoad(final MemoryInstr memoryInstr) {
    final var operands = memoryInstr.getOperands();
    if (operands.isEmpty()) {
      throw new IllegalArgumentException("LOAD instruction requires source operand");
    }

    final var sourceLocation = operands.getFirst();

    // For JVM: Load variable onto stack for consumption by next operation
    // Don't create intermediate variable assignments - just load for stack consumption
    generateLoadVariable(sourceLocation);

    // Track temp variable source if there's a result
    if (memoryInstr.hasResult()) {
      final var resultVar = memoryInstr.getResult();
      trackTempVariableFromLoad(resultVar, sourceLocation);
      generateStoreVariable(resultVar);
    }

    // Note: For JVM backend, we prefer stack operations over variable assignments
    // The temp variable tracking allows us to regenerate the operation as needed
  }

  /**
   * Generate STORE instruction: store value from stack to variable.
   * Format: STORE dest_location = source
   */
  private void generateStore(final MemoryInstr memoryInstr) {
    final var operands = memoryInstr.getOperands();
    if (operands.size() < 2) {
      throw new IllegalArgumentException("STORE instruction requires destination and source operands");
    }

    final var destinationLocation = operands.get(0);
    final var source = operands.get(1);

    // Load source value onto stack
    generateLoadVariable(source);

    // Store to destination
    generateStoreVariable(destinationLocation);
  }

  /**
   * Generate REFERENCE instruction: declare variable reference.
   * Format: REFERENCE variable_name, type_info
   * This is essentially a variable declaration in EK9 IR.
   *
   * IMPORTANT: Method parameters are pre-registered in the variable map and already have values.
   * Only initialize new local variables to null, NOT parameters.
   */
  private void generateReference(final MemoryInstr memoryInstr) {
    final var operands = memoryInstr.getOperands();
    if (operands.size() < 2) {
      throw new IllegalArgumentException("REFERENCE instruction requires variable name and type");
    }

    final var variableName = operands.getFirst();

    // Check if this variable is already in the variable map (= it's a method parameter)
    // If it's a parameter, it already has a value and should NOT be overwritten with null
    if (!isVariableAllocated(variableName)) {
      // This is a new local variable declaration - initialize to null
      getCurrentMethodVisitor().visitInsn(Opcodes.ACONST_NULL);
      generateStoreVariable(variableName);
    }
    // If variable is already allocated (parameter), do nothing - it already has a value
  }

  /**
   * Generate RETAIN instruction: increment reference count (ARC).
   * Format: RETAIN object
   * For JVM, this is typically a no-op since GC handles memory management.
   */
  private void generateRetain(final MemoryInstr memoryInstr) {
    final var operands = memoryInstr.getOperands();
    if (operands.isEmpty()) {
      throw new IllegalArgumentException("RETAIN instruction requires object operand");
    }
  }

  /**
   * Generate RELEASE instruction: decrement reference count (ARC).
   * Format: RELEASE object
   * For JVM, this is typically a no-op since GC handles memory management.
   */
  private void generateRelease(final MemoryInstr memoryInstr) {
    final var operands = memoryInstr.getOperands();
    if (operands.isEmpty()) {
      throw new IllegalArgumentException("RELEASE instruction requires object operand");
    }
  }

  /**
   * Generate IS_NULL instruction: check if object is null.
   * Format: IS_NULL result = operand
   */
  private void generateIsNull(final MemoryInstr memoryInstr) {
    final var operands = memoryInstr.getOperands();
    if (operands.isEmpty()) {
      throw new IllegalArgumentException("IS_NULL instruction requires operand");
    }

    final var operand = operands.getFirst();

    // Load operand onto stack
    generateLoadVariable(operand);

    // Compare with null and produce boolean result
    // This generates: object == null ? 1 : 0
    final var nullLabel = new org.objectweb.asm.Label();
    final var endLabel = new org.objectweb.asm.Label();

    getCurrentMethodVisitor().visitJumpInsn(Opcodes.IFNULL, nullLabel);
    getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_0); // false
    getCurrentMethodVisitor().visitJumpInsn(Opcodes.GOTO, endLabel);

    getCurrentMethodVisitor().visitLabel(nullLabel);
    getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_1); // true

    getCurrentMethodVisitor().visitLabel(endLabel);

    // Convert to EK9 Boolean and store result
    // Use proper IR type conversion for EK9 Boolean
    final var jvmBooleanType = convertToJvmName("org.ek9.lang::Boolean");
    final var booleanDescriptor = convertToJvmDescriptor("org.ek9.lang::Boolean");

    // Convert boolean to String first, then use _of method
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        "java/lang/Boolean",
        "toString",
        "(Z)Ljava/lang/String;",
        false
    );

    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        jvmBooleanType,
        "_of",
        "(Ljava/lang/String;)" + booleanDescriptor,
        false
    );

    if (memoryInstr.hasResult()) {
      generateStoreVariable(memoryInstr.getResult());
    }
  }

  /**
   * Generate FUNCTION_INSTANCE instruction: get singleton function instance.
   * Format: FUNCTION_INSTANCE result = FunctionType
   */
  private void generateFunctionInstance(final MemoryInstr memoryInstr) {
    final var operands = memoryInstr.getOperands();
    if (operands.isEmpty()) {
      throw new IllegalArgumentException("FUNCTION_INSTANCE instruction requires function type");
    }

    final var functionType = operands.getFirst();
    final var jvmTypeName = convertToJvmName(functionType);

    // Call static getInstance() method on the function type
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        jvmTypeName,
        "getInstance",
        "()L" + jvmTypeName + ";",
        false
    );

    // Store result
    if (memoryInstr.hasResult()) {
      generateStoreVariable(memoryInstr.getResult());
    }
  }
}