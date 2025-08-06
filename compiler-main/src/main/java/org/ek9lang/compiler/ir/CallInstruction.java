package org.ek9lang.compiler.ir;

import java.util.Arrays;
import java.util.List;
import org.ek9lang.compiler.common.INodeVisitor;

/**
 * Specialized IR instruction for method calls (CALL, CALL_VIRTUAL, CALL_STATIC, CALL_DISPATCHER).
 * <p>
 * This instruction type handles ALL EK9 method calls and operators.
 * EK9 operators like a + b become method calls like a._add(b).
 * </p>
 */
public final class CallInstruction extends IRInstruction {

  private final String targetObject;
  private final String methodName;
  private final List<String> arguments;

  /**
   * Create standard method call: CALL result = object.method(args...)
   */
  public static CallInstruction call(final String result, final String targetObject, 
                                     final String methodName, final String... arguments) {
    return new CallInstruction(IROpcode.CALL, result, targetObject, methodName, null, Arrays.asList(arguments));
  }

  /**
   * Create standard method call with debug info: CALL result = object.method(args...)
   */
  public static CallInstruction call(final String result, final String targetObject, 
                                     final String methodName, final DebugInfo debugInfo, final String... arguments) {
    return new CallInstruction(IROpcode.CALL, result, targetObject, methodName, debugInfo, Arrays.asList(arguments));
  }

  /**
   * Create virtual method call: CALL_VIRTUAL result = object.method(args...)
   */
  public static CallInstruction callVirtual(final String result, final String targetObject, 
                                             final String methodName, final String... arguments) {
    return new CallInstruction(IROpcode.CALL_VIRTUAL, result, targetObject, methodName, null, Arrays.asList(arguments));
  }

  /**
   * Create virtual method call with debug info: CALL_VIRTUAL result = object.method(args...)
   */
  public static CallInstruction callVirtual(final String result, final String targetObject, 
                                             final String methodName, final DebugInfo debugInfo, final String... arguments) {
    return new CallInstruction(IROpcode.CALL_VIRTUAL, result, targetObject, methodName, debugInfo, Arrays.asList(arguments));
  }

  /**
   * Create static method call: CALL_STATIC result = Type.method(args...)
   */
  public static CallInstruction callStatic(final String result, final String targetType, 
                                            final String methodName, final String... arguments) {
    return new CallInstruction(IROpcode.CALL_STATIC, result, targetType, methodName, null, Arrays.asList(arguments));
  }

  /**
   * Create static method call with debug info: CALL_STATIC result = Type.method(args...)
   */
  public static CallInstruction callStatic(final String result, final String targetType, 
                                            final String methodName, final DebugInfo debugInfo, final String... arguments) {
    return new CallInstruction(IROpcode.CALL_STATIC, result, targetType, methodName, debugInfo, Arrays.asList(arguments));
  }

  /**
   * Create dispatcher method call: CALL_DISPATCHER result = object.dispatcherMethod(args...)
   */
  public static CallInstruction callDispatcher(final String result, final String targetObject, 
                                                final String methodName, final String... arguments) {
    return new CallInstruction(IROpcode.CALL_DISPATCHER, result, targetObject, methodName, null, Arrays.asList(arguments));
  }

  /**
   * Create dispatcher method call with debug info: CALL_DISPATCHER result = object.dispatcherMethod(args...)
   */
  public static CallInstruction callDispatcher(final String result, final String targetObject, 
                                                final String methodName, final DebugInfo debugInfo, final String... arguments) {
    return new CallInstruction(IROpcode.CALL_DISPATCHER, result, targetObject, methodName, debugInfo, Arrays.asList(arguments));
  }

  /**
   * Create EK9 operator call: CALL result = object._operatorMethod(args...)
   * Common operators: _add, _sub, _eq, _lt, _addAss, _isSet, etc.
   */
  public static CallInstruction operator(final String result, final String targetObject, 
                                         final String operatorMethod, final String... arguments) {
    return new CallInstruction(IROpcode.CALL, result, targetObject, operatorMethod, null, Arrays.asList(arguments));
  }

  /**
   * Create EK9 operator call with debug info: CALL result = object._operatorMethod(args...)
   */
  public static CallInstruction operator(final String result, final String targetObject, 
                                         final String operatorMethod, final DebugInfo debugInfo, final String... arguments) {
    return new CallInstruction(IROpcode.CALL, result, targetObject, operatorMethod, debugInfo, Arrays.asList(arguments));
  }

  /**
   * Create constructor call: CALL result = Type.&lt;init&gt;(args...)
   */
  public static CallInstruction constructor(final String result, final String typeName, final String... arguments) {
    return new CallInstruction(IROpcode.CALL, result, typeName, "<init>", null, Arrays.asList(arguments));
  }

  /**
   * Create constructor call with debug info: CALL result = Type.&lt;init&gt;(args...)
   */
  public static CallInstruction constructor(final String result, final String typeName, final DebugInfo debugInfo, final String... arguments) {
    return new CallInstruction(IROpcode.CALL, result, typeName, "<init>", debugInfo, Arrays.asList(arguments));
  }

  private CallInstruction(final IROpcode opcode, final String result, final String targetObject, 
                          final String methodName, final DebugInfo debugInfo, final List<String> arguments) {
    super(opcode, result, debugInfo);
    this.targetObject = targetObject;
    this.methodName = methodName;
    this.arguments = List.copyOf(arguments);
    
    // Build operands: target.method(arg1, arg2, ...)
    super.addOperand(formatMethodCall());
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  private String formatMethodCall() {
    StringBuilder sb = new StringBuilder();
    sb.append(targetObject).append(".").append(methodName);
    
    if (!arguments.isEmpty()) {
      sb.append("(");
      sb.append(String.join(", ", arguments));
      sb.append(")");
    } else {
      sb.append("()");
    }
    
    return sb.toString();
  }

  public String getTargetObject() {
    return targetObject;
  }

  public String getMethodName() {
    return methodName;
  }

  public List<String> getArguments() {
    return arguments;
  }

  /**
   * Check if this is an EK9 operator method call.
   */
  public boolean isOperatorCall() {
    return methodName.startsWith("_");
  }

  /**
   * Check if this is a constructor call.
   */
  public boolean isConstructorCall() {
    return "<init>".equals(methodName);
  }
}