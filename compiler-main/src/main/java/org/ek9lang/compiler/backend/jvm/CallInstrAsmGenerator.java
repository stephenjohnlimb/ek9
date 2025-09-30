package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.core.AssertValue;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Specialized ASM generator for CallInstr processing.
 * Handles all EK9 method calls, operators, and constructor invocations
 * using the actual CallInstr typed methods (no string parsing).
 */
public final class CallInstrAsmGenerator extends AbstractAsmGenerator {

  public CallInstrAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                               final OutputVisitor outputVisitor,
                               final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate JVM bytecode for a method call instruction.
   * Uses CallInstr typed methods to extract call details.
   */
  public void generateCall(final CallInstr callInstr) {
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

    // Generate different bytecode based on call type
    switch (callInstr.getOpcode()) {
      case CALL -> generateInstanceCall(targetObject, methodName, arguments, targetTypeName,
          parameterTypes, returnTypeName, isTraitCall);
      case CALL_VIRTUAL -> generateVirtualCall(targetObject, methodName, arguments, targetTypeName,
          parameterTypes, returnTypeName, isTraitCall);
      case CALL_STATIC -> generateStaticCall(methodName, arguments, targetTypeName,
          parameterTypes, returnTypeName);
      case CALL_DISPATCHER -> generateDispatcherCall(targetObject, methodName, arguments, targetTypeName,
          parameterTypes, returnTypeName);
      default -> throw new IllegalArgumentException("Unsupported call opcode: " + callInstr.getOpcode());
    }

    // Store result if instruction has one and track temp variable source
    if (callInstr.hasResult()) {
      final var resultVar = callInstr.getResult();
      // TODO: Track call results as temp variable sources for constructor/method calls
      generateStoreVariable(resultVar);
    }
  }

  /**
   * Generate instance method call (INVOKEVIRTUAL) or constructor call (NEW + INVOKESPECIAL).
   */
  private void generateInstanceCall(final String targetObject,
                                    final String methodName,
                                    final java.util.List<String> arguments,
                                    final String targetTypeName,
                                    final java.util.List<String> parameterTypes,
                                    final String returnTypeName,
                                    final boolean isTraitCall) {

    final var jvmMethodName = convertEk9MethodToJvm(methodName);
    final var jvmOwnerName = convertToJvmName(targetTypeName);

    // Special handling for constructor calls
    if ("<init>".equals(jvmMethodName)) {
      // Constructor call: NEW + DUP + INVOKESPECIAL
      getCurrentMethodVisitor().visitTypeInsn(Opcodes.NEW, jvmOwnerName);
      getCurrentMethodVisitor().visitInsn(Opcodes.DUP);

      // Load arguments onto stack
      for (final var argument : arguments) {
        generateLoadVariable(argument);
      }

      // Generate constructor descriptor (constructors return void)
      final var constructorDescriptor = generateMethodDescriptor(parameterTypes, "org.ek9.lang::Void");

      // Generate INVOKESPECIAL <init>
      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESPECIAL,
          jvmOwnerName,
          "<init>",
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
                                   final java.util.List<String> arguments,
                                   final String targetTypeName,
                                   final java.util.List<String> parameterTypes,
                                   final String returnTypeName,
                                   final boolean isTraitCall) {
    // For now, same as instance call - could be enhanced for interface calls
    generateInstanceCall(targetObject, methodName, arguments, targetTypeName,
        parameterTypes, returnTypeName, isTraitCall);
  }

  /**
   * Generate static method call (INVOKESTATIC).
   */
  private void generateStaticCall(final String methodName,
                                  final java.util.List<String> arguments,
                                  final String targetTypeName,
                                  final java.util.List<String> parameterTypes,
                                  final String returnTypeName) {

    // Load arguments onto stack (no target object for static calls)
    for (final var argument : arguments) {
      generateStackOperation(argument);
    }

    final var jvmMethodName = convertEk9MethodToJvm(methodName);
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
  }

  /**
   * Generate dispatcher method call (for EK9 function objects).
   */
  private void generateDispatcherCall(final String targetObject,
                                      final String methodName,
                                      final java.util.List<String> arguments,
                                      final String targetTypeName,
                                      final java.util.List<String> parameterTypes,
                                      final String returnTypeName) {
    // Dispatcher calls are similar to instance calls but may have special handling
    // Dispatcher targets are never traits (they're dispatchers), so always false
    generateInstanceCall(targetObject, methodName, arguments, targetTypeName,
        parameterTypes, returnTypeName, false);
  }

  /**
   * Convert EK9 method names to JVM method names.
   * Handles special cases like constructors and EK9 lifecycle methods.
   */
  private String convertEk9MethodToJvm(final String ek9MethodName) {
    return switch (ek9MethodName) {
      case "c_init" -> "<clinit>";  // Static initializer
      case "i_init" -> "i_init";    // Instance field initializer (stays same)
      case "<init>" -> "<init>";    // Constructor (already JVM format)
      default -> ek9MethodName;     // Regular methods keep their names
    };
  }
}