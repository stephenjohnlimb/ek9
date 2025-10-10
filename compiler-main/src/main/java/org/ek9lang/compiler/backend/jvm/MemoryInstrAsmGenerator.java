package org.ek9lang.compiler.backend.jvm;

import java.util.Collections;
import java.util.function.Consumer;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.core.AssertValue;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 * Specialized ASM generator for MemoryInstr processing.
 * Handles LOAD, STORE, REFERENCE, RETAIN, RELEASE operations
 * using the actual MemoryInstr methods (no string parsing).
 */
final class MemoryInstrAsmGenerator extends AbstractAsmGenerator
    implements Consumer<MemoryInstr> {

  MemoryInstrAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                          final OutputVisitor outputVisitor,
                          final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate JVM bytecode for a memory operation instruction.
   * Uses MemoryInstr opcode to determine the specific operation.
   */
  @Override
  public void accept(final MemoryInstr memoryInstr) {
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
   * <p>
   * IMPORTANT: Method parameters are pre-registered in the variable map and already have values.
   * Only initialize new local variables to null, NOT parameters.
   * </p>
   * <p>
   * For LocalVariableTable: Capture variable metadata at declaration time.
   * Scope association happens later via SCOPE_REGISTER instruction.
   * </p>
   */
  private void generateReference(final MemoryInstr memoryInstr) {
    final var operands = memoryInstr.getOperands();
    if (operands.size() < 2) {
      throw new IllegalArgumentException("REFERENCE instruction requires variable name and type");
    }

    final var variableName = operands.getFirst();
    final var ek9TypeName = operands.get(1);

    // Collect variable metadata for LocalVariableTable generation
    // Convert EK9 type name to JVM descriptor (e.g., "org.ek9.lang::String" -> "Lorg/ek9/lang/String;")
    final var typeDescriptor = convertToJvmDescriptor(ek9TypeName);

    // Create LocalVariableInfo - scope will be set later by SCOPE_REGISTER
    final var varInfo = new AbstractAsmGenerator.LocalVariableInfo(variableName, typeDescriptor, null);
    getMethodContext().localVariableMetadata.put(variableName, varInfo);

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
    //No-op
  }

  /**
   * Generate RELEASE instruction: decrement reference count (ARC).
   * Format: RELEASE object
   * For JVM, this is typically a no-op since GC handles memory management.
   */
  private void generateRelease(final MemoryInstr memoryInstr) {
    //No-op
  }

  /**
   * Generate IS_NULL instruction: check if object is null.
   * Format: IS_NULL result = operand
   * <p>
   * Produces a primitive int (0 or 1) for use in control flow branching.
   * This is critical for CONTROL_FLOW_CHAIN instructions that expect
   * primitive_condition to be a Java primitive boolean (int).
   * </p>
   * <p>
   * Stack behavior:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (result stored in variable as primitive int)
   * </p>
   */
  private void generateIsNull(final MemoryInstr memoryInstr) {
    final var operands = memoryInstr.getOperands();
    if (operands.isEmpty()) {
      throw new IllegalArgumentException("IS_NULL instruction requires operand");
    }

    final var operand = operands.getFirst();

    // Load operand onto stack
    generateLoadVariable(operand);  // stack: [obj]

    // Compare with null and produce primitive boolean (int: 0 or 1)
    final var nullLabel = new Label();
    final var endLabel = new Label();

    getCurrentMethodVisitor().visitJumpInsn(Opcodes.IFNULL, nullLabel);  // stack: []
    getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_0); // not null = false, stack: [int=0]
    getCurrentMethodVisitor().visitJumpInsn(Opcodes.GOTO, endLabel);

    getCurrentMethodVisitor().visitLabel(nullLabel);
    getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_1); // is null = true, stack: [int=1]

    getCurrentMethodVisitor().visitLabel(endLabel);
    // Stack: [int] from both paths (0 or 1)

    // Store primitive int directly using ISTORE (not ASTORE!)
    if (memoryInstr.hasResult()) {
      final var resultIndex = getVariableIndex(memoryInstr.getResult());
      getCurrentMethodVisitor().visitVarInsn(Opcodes.ISTORE, resultIndex);  // stack: []
    }
    // Post-condition: stack is empty, result is primitive int in variable slot
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
    // Use helper to generate descriptor: () -> functionType
    final var descriptor = generateMethodDescriptor(Collections.emptyList(), functionType);
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        jvmTypeName,
        "getInstance",
        descriptor,
        false
    );

    // Store result
    if (memoryInstr.hasResult()) {
      generateStoreVariable(memoryInstr.getResult());
    }
  }
}