package org.ek9lang.compiler.backend.jvm;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_BOOLEAN;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_CHARACTER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_FLOAT;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_INTEGER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_BOOLEAN_TO_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_CHAR_TO_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_BOOLEAN;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_CHARACTER;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_FLOAT;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_INTEGER;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_EK9_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_FLOAT_TO_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_INT_TO_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_BOOLEAN;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_CHARACTER;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_FLOAT;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_INTEGER;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_BOOLEAN;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_CHARACTER;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_FLOAT;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_INTEGER;
import static org.ek9lang.compiler.support.JVMTypeNames.PARAM_STRING;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.core.AssertValue;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
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
abstract class AbstractAsmGenerator {

  protected final ConstructTargetTuple constructTargetTuple;
  protected final OutputVisitor outputVisitor;
  protected final ClassWriter classWriter;

  // Reuse existing JVM backend utilities for name conversion
  private final FullyQualifiedJvmName jvmNameConverter = new FullyQualifiedJvmName();
  private final JvmDescriptorConverter descriptorConverter = new JvmDescriptorConverter(jvmNameConverter);

  // Current method visitor being written to
  private MethodVisitor currentMethodVisitor;

  // Shared method context that must be shared across all generators for a method
  private MethodContext methodContext;

  /**
   * Shared context for a method that must be coordinated across all generators.
   */
  public static class MethodContext {
    final Map<String, Integer> variableMap = new HashMap<>();
    final Map<String, TempVariableSource> tempSourceMap = new HashMap<>();
    final Map<String, Label> labelMap = new HashMap<>();
    final Map<String, LocalVariableInfo> localVariableMetadata = new HashMap<>();
    final Map<String, ScopeInfo> scopeMap = new HashMap<>();
    int nextVariableSlot = 1; // Reserve slot 0 for 'this'
  }

  /**
   * Metadata for a local variable needed for LocalVariableTable generation.
   */
  static class LocalVariableInfo {
    final String name;
    final String typeDescriptor;
    final String scopeId;
    int slot;  // JVM local variable slot (filled later)

    public LocalVariableInfo(final String name, final String typeDescriptor, final String scopeId) {
      this.name = name;
      this.typeDescriptor = typeDescriptor;
      this.scopeId = scopeId;
      this.slot = -1;  // Will be set when variable is allocated
    }
  }

  /**
   * Scope boundary information for tracking variable lifetimes.
   */
  static class ScopeInfo {
    final String scopeId;
    Label startLabel;
    Label endLabel;

    public ScopeInfo(final String scopeId) {
      this.scopeId = scopeId;
    }
  }

  /**
   * Represents the source of a temp variable for stack-oriented code generation.
   */
  static class TempVariableSource {
    public enum Type {LOAD_FROM_VARIABLE, LITERAL_VALUE, CONSTRUCTOR_CALL, METHOD_CALL}

    final Type type;
    final String sourceVariable; // For LOAD_FROM_VARIABLE
    final String literalValue;   // For LITERAL_VALUE
    final String literalType;    // For LITERAL_VALUE

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

  /**
   * Validated LocalVariableTable entry ready for JVM emission.
   * All validation completed - labels and slot are guaranteed non-null.
   */
  private record LocalVariableEntry(String name, String descriptor,
                                     Label start, Label end, int slot) {
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
   * Get the shared method context.
   * Provides access to variable maps, label maps, and local variable metadata.
   */
  protected MethodContext getMethodContext() {
    return methodContext;
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
   * Delegates to JvmDescriptorConverter for consistent conversion logic.
   * Handles Java primitive types (boolean, byte, char, short, int, long, float, double).
   */
  protected String convertToJvmDescriptor(final String ek9TypeName) {
    return descriptorConverter.apply(ek9TypeName);
  }

  /**
   * Generate debug line number from EK9 debug info if available.
   * Creates a JVM Label to mark the bytecode position and associates it with the source line number.
   */
  protected void generateDebugInfo(final DebugInfo debugInfo) {
    if (debugInfo != null && debugInfo.isValidLocation()) {
      // Create label for this source line position
      final var lineLabel = new Label();
      getCurrentMethodVisitor().visitLabel(lineLabel);
      getCurrentMethodVisitor().visitLineNumber(debugInfo.lineNumber(), lineLabel);
    }
  }

  /**
   * Find the operation-level scope (method-wide scope) for parameter variables.
   * This is the first scope created via enterMethodScope() before any block scopes.
   *
   * @return Optional containing the method-wide scope, or empty if no valid scope exists
   */
  private java.util.Optional<ScopeInfo> findMethodWideScope() {
    return methodContext.scopeMap.values().stream()
        .filter(scope -> scope.startLabel != null && scope.endLabel != null)
        .findFirst();
  }

  /**
   * Build a validated LocalVariableTable entry for a variable.
   * Performs all validation: scope resolution, label extraction, slot allocation.
   *
   * @param varInfo     Variable metadata to build entry for
   * @param methodScope Method-wide scope for parameters (may be null)
   * @return Optional containing validated entry, or empty if validation fails
   */
  private java.util.Optional<LocalVariableEntry> buildLocalVariableEntry(
      final LocalVariableInfo varInfo, final ScopeInfo methodScope) {

    // Resolve scope labels (parameters vs local variables)
    final Label startLabel;
    final Label endLabel;

    if (varInfo.scopeId == null) {
      // Method parameter without SCOPE_REGISTER - use method-wide scope
      if (methodScope == null) {
        return java.util.Optional.empty();
      }
      startLabel = methodScope.startLabel;
      endLabel = methodScope.endLabel;
    } else {
      // Local variable with explicit scope - use its specific scope
      final var scopeInfo = methodContext.scopeMap.get(varInfo.scopeId);
      if (scopeInfo == null || scopeInfo.startLabel == null || scopeInfo.endLabel == null) {
        return java.util.Optional.empty();
      }
      startLabel = scopeInfo.startLabel;
      endLabel = scopeInfo.endLabel;
    }

    // Resolve JVM slot (update varInfo if needed)
    final int slot;
    if (varInfo.slot != -1) {
      slot = varInfo.slot;
    } else {
      final var slotNumber = methodContext.variableMap.get(varInfo.name);
      if (slotNumber == null) {
        return java.util.Optional.empty();
      }
      varInfo.slot = slotNumber;  // Cache for future use
      slot = slotNumber;
    }

    // All validation passed - return validated entry
    return java.util.Optional.of(
        new LocalVariableEntry(varInfo.name, varInfo.typeDescriptor, startLabel, endLabel, slot)
    );
  }

  /**
   * Generate LocalVariableTable entries for all variables tracked during method processing.
   * Must be called after method body processing but before visitMaxs().
   * <p>
   * LocalVariableTable enables jdb debugger to show variable names and values via 'locals' command.
   * Each entry maps: variable name + type descriptor + scope (start/end labels) + JVM slot.
   * </p>
   * <p>
   * Handles two cases:
   * 1. Method parameters: Have REFERENCE but no SCOPE_REGISTER (caller-owned memory)
   * 2. Local variables: Have REFERENCE + SCOPE_REGISTER (callee-owned memory)
   * </p>
   */
  protected void generateLocalVariableTable() {
    final var mv = getCurrentMethodVisitor();
    final var methodScope = findMethodWideScope().orElse(null);

    // Generate LocalVariableTable entry for each valid variable
    // Uses functional decomposition: validation in helper, emission here
    methodContext.localVariableMetadata.values().forEach(varInfo ->
        buildLocalVariableEntry(varInfo, methodScope).ifPresent(entry ->
            mv.visitLocalVariable(
                entry.name(),
                entry.descriptor(),
                null,  // signature - null for non-generic types
                entry.start(),
                entry.end(),
                entry.slot()
            )
        )
    );
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
   * Example: _temp1 -&gt; slot 1, _temp10 -&gt; slot 2, _temp3 -&gt; slot 3 (in order encountered)
   * </p>
   */
  @SuppressWarnings("checkstyle:LambdaParameterName")
  protected int getVariableIndex(final String variableName) {
    return methodContext.variableMap.computeIfAbsent(variableName, _ -> methodContext.nextVariableSlot++);
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
   * Get or create JVM Label for IR label name.
   * Reuses the same Label object for both label definition (LABEL instruction)
   * and branch targets (BRANCH, BRANCH_TRUE, BRANCH_FALSE instructions).
   */
  @SuppressWarnings("checkstyle:LambdaParameterName")
  protected Label getOrCreateLabel(final String labelName) {
    return methodContext.labelMap.computeIfAbsent(labelName, _ -> new Label());
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
      case EK9_STRING -> generateStringLiteral(literalValue);
      case EK9_INTEGER -> generateIntegerLiteral(literalValue);
      case EK9_BOOLEAN -> generateBooleanLiteral(literalValue);
      case EK9_FLOAT -> generateFloatLiteral(literalValue);
      case EK9_CHARACTER -> generateCharacterLiteral(literalValue);
      default -> generateObjectLiteral(literalValue, literalType);
    }
  }

  /**
   * Generate EK9 String literal from string value.
   */
  protected void generateStringLiteral(final String literalValue) {
    // Generate String literal directly onto stack
    getCurrentMethodVisitor().visitLdcInsn(literalValue);

    // Convert to EK9 String
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        EK9_LANG_STRING,
        "_of",
        PARAM_STRING + DESC_EK9_STRING,
        false
    );
  }

  /**
   * Generate EK9 Integer literal from string value.
   */
  protected void generateIntegerLiteral(final String literalValue) {
    try {
      final int intValue = Integer.parseInt(literalValue);
      // Load int constant using size-optimized JVM instructions:
      // ICONST_* = 1 byte (fastest, for common values -1 to 5)
      // BIPUSH = 2 bytes (for byte range -128 to 127)
      // SIPUSH = 3 bytes (for short range -32768 to 32767)
      // LDC = 2 bytes + constant pool entry (for larger values)
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
          JAVA_LANG_INTEGER,
          "toString",
          DESC_INT_TO_STRING,
          false
      );
      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          EK9_LANG_INTEGER,
          "_of",
          PARAM_STRING + DESC_EK9_INTEGER,
          false
      );
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid integer literal: " + literalValue, e);
    }
  }

  /**
   * Generate EK9 Boolean literal from string value.
   */
  protected void generateBooleanLiteral(final String literalValue) {
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
        JAVA_LANG_BOOLEAN,
        "toString",
        DESC_BOOLEAN_TO_STRING,
        false
    );
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        EK9_LANG_BOOLEAN,
        "_of",
        PARAM_STRING + DESC_EK9_BOOLEAN,
        false
    );
  }

  /**
   * Generate EK9 Float literal from string value.
   */
  protected void generateFloatLiteral(final String literalValue) {
    try {
      final float floatValue = Float.parseFloat(literalValue);

      // Use FCONST_0 (1 byte) for zero, which is common in initializers
      // Use LDC (2 bytes) for all other values - optimization for 1.0f/2.0f not worth the complexity
      if (floatValue == 0.0f) {
        getCurrentMethodVisitor().visitInsn(Opcodes.FCONST_0);
      } else {
        getCurrentMethodVisitor().visitLdcInsn(floatValue);
      }

      // Convert float to String first, then use _of method to create EK9 Float
      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          JAVA_LANG_FLOAT,
          "toString",
          DESC_FLOAT_TO_STRING,
          false
      );

      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          EK9_LANG_FLOAT,
          "_of",
          PARAM_STRING + DESC_EK9_FLOAT,
          false
      );

    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid float literal: " + literalValue, e);
    }
  }

  /**
   * Generate EK9 Character literal from string value.
   */
  protected void generateCharacterLiteral(final String literalValue) {
    if (literalValue.length() == 1) {
      final char charValue = literalValue.charAt(0);
      getCurrentMethodVisitor().visitIntInsn(Opcodes.BIPUSH, charValue);

      // Convert char to String first, then use _of method to create EK9 Character
      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          JAVA_LANG_CHARACTER,
          "toString",
          DESC_CHAR_TO_STRING,
          false
      );

      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          EK9_LANG_CHARACTER,
          "_of",
          PARAM_STRING + DESC_EK9_CHARACTER,
          false
      );
    } else {
      throw new IllegalArgumentException("Invalid character literal: " + literalValue);
    }
  }

  /**
   * Generate EK9 type literal from string value for generic/object types.
   * This handles complex literals that don't fit standard primitive categories.
   */
  protected void generateObjectLiteral(final String literalValue, final String literalType) {
    // For other types, treat as string and let the target type constructor handle parsing
    getCurrentMethodVisitor().visitLdcInsn(literalValue);
    final var jvmTypeName = convertToJvmName(literalType);
    final var typeDescriptor = convertToJvmDescriptor(literalType);
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        jvmTypeName,
        "_of",
        PARAM_STRING + typeDescriptor,
        false
    );
  }

  /**
   * Generate method descriptor from EK9 parameter and return types.
   */
  protected String generateMethodDescriptor(final List<String> parameterTypes,
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
   * Determine if a temp variable should remain on stack instead of being stored.
   * For stack-oriented approach, temp variables that are only used once should remain on stack.
   */
  private boolean shouldRemainOnStack(final String variableName) {
    // For now, disable stack optimization until we can properly track usage patterns
    // TODO: Implement proper single-use analysis for temp variables
    return false;
  }

  /**
   * Convert Java primitive boolean on stack to int (0 or 1) via Boolean wrapper.
   * <p>
   * Java primitive boolean and int are distinct types in JVM verification.
   * Methods like Boolean._true(), _false(), _set() return primitive boolean,
   * but conditional branch instructions require proper type conversion for verification.
   * </p>
   * <p>
   * Solution: Use Boolean.valueOf() boxing to convert boolean→Boolean object,
   * then call intValue() from Boolean's Number superclass to get int.
   * This satisfies JVM verifier's type system requirements.
   * </p>
   * <p>
   * Stack transformation:
   * Pre-condition: [boolean] (primitive boolean type)
   * Post-condition: [int] (primitive int type, value 0 or 1)
   * </p>
   * <p>
   * Bytecode pattern:
   * </p>
   * <pre>
   * // Stack: [boolean]
   * INVOKESTATIC Boolean.valueOf(Z)LBoolean;  // Box to Boolean object
   * INVOKEVIRTUAL Boolean.intValue()I          // Unbox to int via Number.intValue()
   * // Stack: [int]
   * </pre>
   */
  protected void convertBooleanToInt() {
    // Stack: [boolean] - box to Boolean object
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        "java/lang/Boolean",
        "valueOf",
        "(Z)Ljava/lang/Boolean;",
        false
    );
    // Stack: [Boolean object]

    // Unbox to int (Boolean extends Number, which has intValue())
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        "java/lang/Boolean",
        "intValue",
        "()I",
        false
    );
    // Stack: [int] - now safe to use with IFEQ/IFNE
  }
}