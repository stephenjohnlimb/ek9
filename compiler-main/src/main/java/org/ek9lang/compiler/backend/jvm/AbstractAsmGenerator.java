package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.core.AssertValue;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Common abstract base class for all specialized ASM generators.
 * Provides shared functionality for JVM bytecode generation including:
 * - Access to ClassWriter and current MethodVisitor
 * - EK9 to JVM name conversion using existing utilities
 * - Debug information handling
 * - Common JVM instruction patterns
 */
public abstract class AbstractAsmGenerator {

  protected final ConstructTargetTuple constructTargetTuple;
  protected final OutputVisitor outputVisitor;
  protected final ClassWriter classWriter;

  // Reuse existing JVM backend utilities for name conversion
  private final FullyQualifiedJvmName jvmNameConverter = new FullyQualifiedJvmName();

  // Current method visitor being written to
  private MethodVisitor currentMethodVisitor;

  // Shared method context that must be shared across all generators for a method
  private MethodContext methodContext;

  /**
   * Shared context for a method that must be coordinated across all generators.
   */
  public static class MethodContext {
    final java.util.Map<String, Integer> variableMap = new java.util.HashMap<>();
    final java.util.Map<String, TempVariableSource> tempSourceMap = new java.util.HashMap<>();
    int nextVariableSlot = 1; // Reserve slot 0 for 'this'
  }

  /**
   * Represents the source of a temp variable for stack-oriented code generation.
   */
  public static class TempVariableSource {
    public enum Type {LOAD_FROM_VARIABLE, LITERAL_VALUE, CONSTRUCTOR_CALL, METHOD_CALL}

    public final Type type;
    public final String sourceVariable; // For LOAD_FROM_VARIABLE
    public final String literalValue;   // For LITERAL_VALUE
    public final String literalType;    // For LITERAL_VALUE

    // For LOAD_FROM_VARIABLE
    public TempVariableSource(final String sourceVariable) {
      this.type = Type.LOAD_FROM_VARIABLE;
      this.sourceVariable = sourceVariable;
      this.literalValue = null;
      this.literalType = null;
    }

    // For LITERAL_VALUE
    public TempVariableSource(final String literalValue, final String literalType) {
      this.type = Type.LITERAL_VALUE;
      this.sourceVariable = null;
      this.literalValue = literalValue;
      this.literalType = literalType;
    }
  }

  protected AbstractAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                 final OutputVisitor outputVisitor,
                                 final ClassWriter classWriter) {
    AssertValue.checkNotNull("ConstructTargetTuple cannot be null", constructTargetTuple);
    AssertValue.checkNotNull("OutputVisitor cannot be null", outputVisitor);
    AssertValue.checkNotNull("ClassWriter cannot be null", classWriter);

    this.constructTargetTuple = constructTargetTuple;
    this.outputVisitor = outputVisitor;
    this.classWriter = classWriter;

    // Initialize with a local context by default
    this.methodContext = new MethodContext();
  }

  /**
   * Set shared method context for coordinated variable slot allocation across generators.
   * This MUST be called before processing instructions to ensure all generators use the same context.
   */
  protected void setSharedMethodContext(final MethodContext context) {
    this.methodContext = context;
  }

  /**
   * Set the current method visitor for instruction generation.
   * Note: Does NOT reset variable maps - that should be done by the caller if needed.
   */
  protected void setCurrentMethodVisitor(final MethodVisitor methodVisitor) {
    this.currentMethodVisitor = methodVisitor;
    // Do NOT clear maps here - they are shared across generators!
  }

  /**
   * Get the current method visitor.
   */
  protected MethodVisitor getCurrentMethodVisitor() {
    AssertValue.checkNotNull("Method visitor must be set before use", currentMethodVisitor);
    return currentMethodVisitor;
  }

  /**
   * Convert EK9 fully qualified name to JVM internal name format.
   * Uses existing FullyQualifiedJvmName utility.
   *
   * @param ek9FullyQualifiedName EK9 name like "org.ek9.lang::String"
   * @return JVM internal name like "org/ek9/lang/String"
   */
  protected String convertToJvmName(final String ek9FullyQualifiedName) {
    return jvmNameConverter.apply(ek9FullyQualifiedName);
  }

  /**
   * Convert EK9 type name to JVM descriptor format.
   * Uses FullyQualifiedJvmName utility for consistent conversion.
   */
  protected String convertToJvmDescriptor(final String ek9TypeName) {
    // Handle void type specially
    if ("org.ek9.lang::Void".equals(ek9TypeName)) {
      return "V";
    }

    // Use existing utility for all other types
    return "L" + convertToJvmName(ek9TypeName) + ";";
  }

  /**
   * Generate debug line number from EK9 debug info if available.
   */
  protected void generateDebugInfo(final DebugInfo debugInfo) {
    if (debugInfo != null && debugInfo.isValidLocation()) {
      // For now, skip debug line number generation as it requires proper label handling
      // TODO: Implement proper label management for debug line numbers
      // getCurrentMethodVisitor().visitLineNumber(debugInfo.lineNumber(), null);
    }
  }

  /**
   * Generate common JVM instructions for loading a variable.
   */
  protected void generateLoadVariable(final String variableName) {
    // For now, generate simple ALOAD for object references
    // This will be enhanced based on variable type and storage location
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ALOAD, getVariableIndex(variableName));
  }

  /**
   * Generate common JVM instructions for storing to a variable.
   * In stack-oriented approach, temp variables that will be consumed immediately
   * should not be stored to local variables at all.
   */
  protected void generateStoreVariable(final String variableName) {
    // Check if this is a temp variable that should remain on stack
    if (shouldRemainOnStack(variableName)) {
      // Don't store - leave value on stack for immediate consumption
      return;
    }

    // For regular variables, generate simple ASTORE for object references
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ASTORE, getVariableIndex(variableName));
  }

  /**
   * Get or assign local variable slot for a variable name.
   * Uses opaque identifier mapping - treats variable names as unique identifiers
   * and assigns sequential slots per method (1, 2, 3, 4, etc.) regardless of the actual name.
   * <p>
   * Example: _temp1 -> slot 1, _temp10 -> slot 2, _temp3 -> slot 3 (in order encountered)
   */
  protected int getVariableIndex(final String variableName) {
    return methodContext.variableMap.computeIfAbsent(variableName, name -> methodContext.nextVariableSlot++);
  }

  /**
   * Check if a variable has already been allocated a local variable slot.
   * Returns true for method parameters (pre-registered) and already-declared local variables.
   * This is used to distinguish parameters from new local variable declarations.
   */
  protected boolean isVariableAllocated(final String variableName) {
    return methodContext.variableMap.containsKey(variableName);
  }

  /**
   * Track that a temp variable was created from loading another variable.
   */
  protected void trackTempVariableFromLoad(final String tempVar, final String sourceVar) {
    methodContext.tempSourceMap.put(tempVar, new TempVariableSource(sourceVar));
  }

  /**
   * Track that a temp variable was created from a literal value.
   */
  protected void trackTempVariableFromLiteral(final String tempVar, final String literalValue,
                                              final String literalType) {
    methodContext.tempSourceMap.put(tempVar, new TempVariableSource(literalValue, literalType));
  }

  /**
   * Generate stack operation for a variable - either load from slot or re-generate the operation.
   * This is the key method for stack-oriented code generation.
   */
  protected void generateStackOperation(final String variableName) {
    final var tempSource = methodContext.tempSourceMap.get(variableName);

    if (tempSource != null) {
      // This is a temp variable - re-generate its source operation as stack operation
      switch (tempSource.type) {
        case LOAD_FROM_VARIABLE -> generateLoadVariable(tempSource.sourceVariable);
        case LITERAL_VALUE -> generateLiteralStackOperation(tempSource.literalValue, tempSource.literalType);
        default -> generateLoadVariable(variableName);
      }
    } else {
      // Regular variable - load from its slot
      generateLoadVariable(variableName);
    }
  }

  /**
   * Generate literal value directly onto stack (for stack-oriented approach).
   */
  private void generateLiteralStackOperation(final String literalValue, final String literalType) {
    switch (literalType) {
      case "org.ek9.lang::String" -> {
        // Generate String literal directly onto stack
        getCurrentMethodVisitor().visitLdcInsn(literalValue);

        // Convert to EK9 String
        final var jvmStringType = convertToJvmName("org.ek9.lang::String");
        final var stringDescriptor = convertToJvmDescriptor("org.ek9.lang::String");
        getCurrentMethodVisitor().visitMethodInsn(
            Opcodes.INVOKESTATIC,
            jvmStringType,
            "_of",
            "(Ljava/lang/String;)" + stringDescriptor,
            false
        );
      }
      case "org.ek9.lang::Integer" -> {
        try {
          final int intValue = Integer.parseInt(literalValue);
          // Load int constant
          switch (intValue) {
            case -1 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_M1);
            case 0 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_0);
            case 1 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_1);
            case 2 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_2);
            case 3 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_3);
            case 4 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_4);
            case 5 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_5);
            default -> {
              if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
                getCurrentMethodVisitor().visitIntInsn(Opcodes.BIPUSH, intValue);
              } else if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
                getCurrentMethodVisitor().visitIntInsn(Opcodes.SIPUSH, intValue);
              } else {
                getCurrentMethodVisitor().visitLdcInsn(intValue);
              }
            }
          }
          // Convert int to String then to EK9 Integer
          getCurrentMethodVisitor().visitMethodInsn(
              Opcodes.INVOKESTATIC,
              "java/lang/Integer",
              "toString",
              "(I)Ljava/lang/String;",
              false
          );
          final var jvmIntegerType = convertToJvmName("org.ek9.lang::Integer");
          final var integerDescriptor = convertToJvmDescriptor("org.ek9.lang::Integer");
          getCurrentMethodVisitor().visitMethodInsn(
              Opcodes.INVOKESTATIC,
              jvmIntegerType,
              "_of",
              "(Ljava/lang/String;)" + integerDescriptor,
              false
          );
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Invalid integer literal: " + literalValue, e);
        }
      }
      case "org.ek9.lang::Boolean" -> {
        // Load boolean value as int (0 or 1)
        if ("true".equals(literalValue)) {
          getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_1);
        } else if ("false".equals(literalValue)) {
          getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_0);
        } else {
          throw new IllegalArgumentException("Invalid boolean literal: " + literalValue);
        }
        // Convert boolean to String then to EK9 Boolean
        getCurrentMethodVisitor().visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/lang/Boolean",
            "toString",
            "(Z)Ljava/lang/String;",
            false
        );
        final var jvmBooleanType = convertToJvmName("org.ek9.lang::Boolean");
        final var booleanDescriptor = convertToJvmDescriptor("org.ek9.lang::Boolean");
        getCurrentMethodVisitor().visitMethodInsn(
            Opcodes.INVOKESTATIC,
            jvmBooleanType,
            "_of",
            "(Ljava/lang/String;)" + booleanDescriptor,
            false
        );
      }
      default -> {
        // For other types, treat as string and let the target type constructor handle parsing
        getCurrentMethodVisitor().visitLdcInsn(literalValue);
        final var jvmTypeName = convertToJvmName(literalType);
        final var typeDescriptor = convertToJvmDescriptor(literalType);
        getCurrentMethodVisitor().visitMethodInsn(
            Opcodes.INVOKESTATIC,
            jvmTypeName,
            "_of",
            "(Ljava/lang/String;)" + typeDescriptor,
            false
        );
      }
    }
  }

  /**
   * Generate method descriptor from EK9 parameter and return types.
   */
  protected String generateMethodDescriptor(final java.util.List<String> parameterTypes,
                                            final String returnType) {
    final var descriptor = new StringBuilder("(");

    for (final var paramType : parameterTypes) {
      descriptor.append(convertToJvmDescriptor(paramType));
    }

    descriptor.append(")");
    descriptor.append(convertToJvmDescriptor(returnType));

    return descriptor.toString();
  }

  /**
   * Check if method name is a JVM special method.
   */
  protected boolean isSpecialMethod(final String methodName) {
    return "<init>".equals(methodName) || "<clinit>".equals(methodName);
  }

  /**
   * Determine if a temp variable should remain on stack instead of being stored.
   * For stack-oriented approach, temp variables that are only used once should remain on stack.
   */
  private boolean shouldRemainOnStack(final String variableName) {
    // For now, disable stack optimization until we can properly track usage patterns
    // TODO: Implement proper single-use analysis for temp variables
    return false;
  }
}