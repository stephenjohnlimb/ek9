package org.ek9lang.compiler.backend.jvm;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_VOID;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_ANY;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_OBJECT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_CLINIT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_C_INIT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_INIT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_I_INIT;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.core.AssertValue;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Specialized ASM generator for CallInstr processing.
 * Handles all EK9 method calls, operators, and constructor invocations
 * using the actual CallInstr typed methods (no string parsing).
 */
final class CallInstrAsmGenerator extends AbstractAsmGenerator
    implements Consumer<CallInstr> {

  private final OperatorMap operatorMap = new OperatorMap();

  CallInstrAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                        final OutputVisitor outputVisitor,
                        final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate JVM bytecode for a method call instruction.
   * Uses CallInstr typed methods to extract call details.
   */
  @Override
  public void accept(final CallInstr callInstr) {
    AssertValue.checkNotNull("CallInstr cannot be null", callInstr);

    // Generate debug info if available
    callInstr.getDebugInfo().ifPresent(this::generateDebugInfo);

    // Extract call details using typed methods (not string parsing)
    final var targetObject = callInstr.getTargetObject();
    final var methodName = callInstr.getMethodName();
    final var arguments = callInstr.getArguments();
    final var targetTypeName = callInstr.getTargetTypeName();
    final var parameterTypes = callInstr.getParameterTypes();
    final var returnTypeName = callInstr.getReturnTypeName();
    final var isTraitCall = callInstr.isTraitCall();

    // CRITICAL: Detect super/this constructor calls BEFORE routing to normal call handlers
    // Super/this calls must use ALOAD_0 + INVOKESPECIAL (not NEW + DUP + INVOKESPECIAL)
    if (isSuperOrThisCall(callInstr)) {
      generateSuperConstructorCall(targetTypeName, arguments, parameterTypes);
      // Result is stored in accept() after switch, no special handling needed
      return;
    }

    // Generate different bytecode based on call type
    // Track whether bytecode was actually generated (false if call was skipped like <clinit>)
    boolean bytecodeGenerated = true;
    switch (callInstr.getOpcode()) {
      case CALL -> generateInstanceCall(targetObject, methodName, arguments, targetTypeName,
          parameterTypes, returnTypeName, isTraitCall);
      case CALL_VIRTUAL -> generateVirtualCall(targetObject, methodName, arguments, targetTypeName,
          parameterTypes, returnTypeName, isTraitCall);
      case CALL_STATIC -> bytecodeGenerated = generateStaticCall(methodName, arguments, targetTypeName,
          parameterTypes, returnTypeName);
      case CALL_DISPATCHER -> generateDispatcherCall(targetObject, methodName, arguments, targetTypeName,
          parameterTypes, returnTypeName);
      default -> throw new IllegalArgumentException("Unsupported call opcode: " + callInstr.getOpcode());
    }

    // Store result if instruction has one AND bytecode was actually generated
    if (callInstr.hasResult() && bytecodeGenerated) {
      final var resultVar = callInstr.getResult();

      // Handle special methods that return Java primitive boolean
      // Methods like _true(), _false(), _set() return primitive boolean
      // At JVM bytecode level, boolean and int are the same (both 32-bit)
      // Just use ISTORE directly - no conversion needed
      if (isJavaPrimitiveBooleanMethod(methodName)) {
        // Stack: [boolean] from method call
        // Store as int using ISTORE (boolean and int are same at bytecode level)
        final var resultIndex = getVariableIndex(resultVar);
        getCurrentMethodVisitor().visitVarInsn(Opcodes.ISTORE, resultIndex);
        // Stack: [] - value stored in local variable slot
      } else {
        // Regular object return - use standard storage
        // TODO: Track call results as temp variable sources for constructor/method calls
        generateStoreVariable(resultVar);
      }
    }
  }

  /**
   * Check if method returns Java primitive boolean (not EK9 Boolean object).
   * These special utility methods require boolean→int conversion for JVM verification.
   * <p>
   * Java primitive boolean and int are distinct types in JVM verification.
   * Methods returning primitive boolean must be converted to int (0/1) before
   * storage/branching to maintain type consistency.
   * </p>
   *
   * @param methodName The method name to check
   * @return true if method returns Java primitive boolean
   */
  private boolean isJavaPrimitiveBooleanMethod(final String methodName) {
    return switch (methodName) {
      case "_true", "_false", "_set" -> true;
      default -> false;
    };
  }

  /**
   * Detect if this is a super or this constructor call (not regular object construction).
   * <p>
   * Super/this constructor calls in constructor bodies have special characteristics:
   * - Target is "super" (for super calls) or "this" (for this calls)
   * - Method name is "&lt;init&gt;"
   * </p>
   * <p>
   * These calls should NOT use NEW + DUP, but instead ALOAD_0 + INVOKESPECIAL.
   * </p>
   * <p>
   * This detection works for both synthetic and explicit constructors. The target and method
   * name alone are sufficient to distinguish super/this calls from regular object construction:
   * - Regular construction: new MyClass() → target = type name (not "super"/"this")
   * - Super constructor: super(args) → target = "super", method = "&lt;init&gt;"
   * - This constructor: this(args) → target = "this", method = "&lt;init&gt;"
   * - Super method call: super.someMethod() → target = "super", method ≠ "&lt;init&gt;"
   * </p>
   *
   * @param callInstr The call instruction to check
   * @return true if this is a super/this constructor call
   */
  private boolean isSuperOrThisCall(final CallInstr callInstr) {
    final var target = callInstr.getTargetObject();
    final var methodName = callInstr.getMethodName();

    return METHOD_INIT.equals(methodName)
        && ("super".equals(target) || "this".equals(target));
  }

  /**
   * Generate instance method call (INVOKEVIRTUAL) or constructor call (NEW + INVOKESPECIAL).
   */
  private void generateInstanceCall(final String targetObject,
                                    final String methodName,
                                    final List<String> arguments,
                                    final String targetTypeName,
                                    final List<String> parameterTypes,
                                    final String returnTypeName,
                                    final boolean isTraitCall) {

    final var jvmMethodName = convertEk9MethodToJvm(methodName);
    final var jvmOwnerName = convertToJvmName(targetTypeName);

    // Special handling for constructor calls
    if (METHOD_INIT.equals(jvmMethodName)) {
      // Constructor call: NEW + DUP + INVOKESPECIAL
      getCurrentMethodVisitor().visitTypeInsn(Opcodes.NEW, jvmOwnerName);
      getCurrentMethodVisitor().visitInsn(Opcodes.DUP);

      // Load arguments onto stack
      for (final var argument : arguments) {
        generateLoadVariable(argument);
      }

      // Generate constructor descriptor (constructors return void)
      final var constructorDescriptor = generateMethodDescriptor(parameterTypes, EK9_VOID);

      // Generate INVOKESPECIAL <init>
      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESPECIAL,
          jvmOwnerName,
          METHOD_INIT,
          constructorDescriptor,
          false
      );
    } else {
      // Regular instance method call: INVOKEVIRTUAL

      // Load target object onto stack
      if (!"this".equals(targetObject)) {
        generateLoadVariable(targetObject);
      } else {
        getCurrentMethodVisitor().visitVarInsn(Opcodes.ALOAD, 0); // Load 'this'
      }

      // Load arguments onto stack
      for (final var argument : arguments) {
        generateLoadVariable(argument);
      }

      final var methodDescriptor = generateMethodDescriptor(parameterTypes, returnTypeName);

      // Generate INVOKEVIRTUAL or INVOKEINTERFACE instruction based on trait flag
      final var opcode = isTraitCall ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL;
      getCurrentMethodVisitor().visitMethodInsn(
          opcode,
          jvmOwnerName,
          jvmMethodName,
          methodDescriptor,
          isTraitCall  // true for traits (interfaces), false for classes
      );
    }
  }

  /**
   * Generate virtual method call (INVOKEVIRTUAL with interface).
   */
  private void generateVirtualCall(final String targetObject,
                                   final String methodName,
                                   final List<String> arguments,
                                   final String targetTypeName,
                                   final List<String> parameterTypes,
                                   final String returnTypeName,
                                   final boolean isTraitCall) {
    // For now, same as instance call - could be enhanced for interface calls
    generateInstanceCall(targetObject, methodName, arguments, targetTypeName,
        parameterTypes, returnTypeName, isTraitCall);
  }

  /**
   * Generate static method call (INVOKESTATIC).
   *
   * @return true if bytecode was generated, false if call was skipped
   */
  private boolean generateStaticCall(final String methodName,
                                     final List<String> arguments,
                                     final String targetTypeName,
                                     final List<String> parameterTypes,
                                     final String returnTypeName) {

    final var jvmMethodName = convertEk9MethodToJvm(methodName);

    // CRITICAL JVM CONSTRAINT: Skip super class c_init calls in static initializers
    // The IR correctly generates CALL_STATIC to parent's c_init for static initialization chaining,
    // but JVM automatically chains <clinit> methods during class loading. Explicit invokestatic <clinit>
    // is illegal and causes ClassFormatError. The JVM class loader handles this automatically:
    // - Parent's <clinit> runs when parent class is loaded (before child)
    // - Child's <clinit> runs when child class is loaded
    // No explicit call needed or allowed.
    if (METHOD_CLINIT.equals(jvmMethodName)) {
      // Skip generating invokestatic <clinit> - JVM handles static initializer chaining automatically
      return false;  // Bytecode not generated
    }

    // Load arguments onto stack (no target object for static calls)
    for (final var argument : arguments) {
      generateStackOperation(argument);
    }

    final var jvmOwnerName = convertToJvmName(targetTypeName);
    final var methodDescriptor = generateMethodDescriptor(parameterTypes, returnTypeName);

    // Generate INVOKESTATIC instruction
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        jvmOwnerName,
        jvmMethodName,
        methodDescriptor,
        false
    );
    return true;  // Bytecode was generated
  }

  /**
   * Generate dispatcher method call (for EK9 function objects).
   */
  private void generateDispatcherCall(final String targetObject,
                                      final String methodName,
                                      final List<String> arguments,
                                      final String targetTypeName,
                                      final List<String> parameterTypes,
                                      final String returnTypeName) {
    // Dispatcher calls are similar to instance calls but may have special handling
    // Dispatcher targets are never traits (they're dispatchers), so always false
    generateInstanceCall(targetObject, methodName, arguments, targetTypeName,
        parameterTypes, returnTypeName, false);
  }

  /**
   * Generate super or this constructor call (INVOKESPECIAL without NEW).
   * <p>
   * Super/this constructor calls are different from object construction:
   * - No NEW instruction (object already exists - it's 'this')
   * - ALOAD_0 to load 'this' onto stack
   * - Load constructor arguments
   * - INVOKESPECIAL SuperClass.&lt;init&gt; or ThisClass.&lt;init&gt;
   * </p>
   * <p>
   * Special handling for Any: translates to Object.&lt;init&gt; since Any is an interface.
   * </p>
   *
   * @param targetTypeName The EK9 type name (may be "Any")
   * @param arguments      Constructor arguments
   * @param parameterTypes Constructor parameter types
   */
  private void generateSuperConstructorCall(final String targetTypeName,
                                            final List<String> arguments,
                                            final List<String> parameterTypes) {

    // Translate Any to Object (Any is an interface, can't have constructors)
    final var jvmOwnerName = EK9_LANG_ANY.equals(convertToJvmName(targetTypeName))
        ? JAVA_LANG_OBJECT
        : convertToJvmName(targetTypeName);

    // Load 'this' onto stack
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ALOAD, 0);

    // Load arguments onto stack
    for (final var argument : arguments) {
      generateLoadVariable(argument);
    }

    // Generate constructor descriptor (constructors return void)
    final var constructorDescriptor = generateMethodDescriptor(parameterTypes, EK9_VOID);

    // Generate INVOKESPECIAL <init> on the superclass
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESPECIAL,
        jvmOwnerName,
        METHOD_INIT,
        constructorDescriptor,
        false
    );
  }

  /**
   * Convert EK9 method names to JVM method names.
   * Handles special cases like constructors and EK9 lifecycle methods.
   */
  private String convertEk9MethodToJvm(final String ek9MethodName) {
    return switch (ek9MethodName) {
      case METHOD_C_INIT -> METHOD_CLINIT; // Static initializer
      case METHOD_I_INIT -> METHOD_I_INIT; // Instance field initializer (stays same)
      case METHOD_INIT -> METHOD_INIT;     // Constructor (already JVM format)
      default -> operatorMap.hasOperator(ek9MethodName)
          ? operatorMap.getForward(ek9MethodName)
          : ek9MethodName;  // Regular methods keep their names
    };
  }
}