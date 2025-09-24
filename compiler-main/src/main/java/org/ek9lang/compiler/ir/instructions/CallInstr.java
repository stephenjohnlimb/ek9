package org.ek9lang.compiler.ir.instructions;

import java.util.List;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.support.VariableDetails;

/**
 * Specialized IR instruction for method calls (CALL, CALL_VIRTUAL, CALL_STATIC, CALL_DISPATCHER).
 * <p>
 * This instruction type handles ALL EK9 method calls and operators.
 * EK9 operators like a + b become method calls like a._add(b).
 * </p>
 */
public final class CallInstr extends IRInstr {

  //The details relating to the call to be made.
  private final CallDetails callDetails;

  /**
   * Create method call with complete type information from resolved symbols: CALL result = object.method(args...)
   */
  public static CallInstr call(final String result,
                               final DebugInfo debugInfo,
                               final CallDetails callDetails) {
    return new CallInstr(IROpcode.CALL, result, debugInfo, callDetails);
  }

  /**
   * Create virtual method call with complete type information: CALL_VIRTUAL result = object.method(args...)
   */
  public static CallInstr callVirtual(final String result,
                                      final DebugInfo debugInfo,
                                      final CallDetails callDetails) {
    return new CallInstr(IROpcode.CALL_VIRTUAL, result, debugInfo, callDetails);
  }

  /**
   * Create static method call with complete type information: CALL_STATIC result = Type.method(args...)
   */
  public static CallInstr callStatic(final String result,
                                     final DebugInfo debugInfo,
                                     final CallDetails callDetails) {
    return new CallInstr(IROpcode.CALL_STATIC, result, debugInfo, callDetails);
  }

  public static CallInstr callStatic(final VariableDetails variableDetails,
                                     final CallDetails callDetails) {
    return new CallInstr(IROpcode.CALL_STATIC,
        variableDetails.resultVariable(), variableDetails.debugInfo(), callDetails);
  }


  /**
   * Create dispatcher method call with complete type information:
   * CALL_DISPATCHER result = object.dispatcherMethod(args...)
   */
  public static CallInstr callDispatcher(final String result,
                                         final DebugInfo debugInfo,
                                         final CallDetails callDetails) {
    return new CallInstr(IROpcode.CALL_DISPATCHER, result, debugInfo, callDetails);
  }

  /**
   * Create EK9 operator call with complete type information: CALL result = object._operatorMethod(args...)
   * Common operators: _add, _sub, _eq, _lt, _addAss, _isSet, etc.
   */
  public static CallInstr operator(final String result,
                                   final DebugInfo debugInfo,
                                   final CallDetails callDetails) {
    return new CallInstr(IROpcode.CALL, result, debugInfo, callDetails);
  }

  public static CallInstr operator(final VariableDetails variableDetails,
                                   final CallDetails callDetails) {
    return new CallInstr(IROpcode.CALL,
        variableDetails.resultVariable(), variableDetails.debugInfo(), callDetails);
  }

  /**
   * Create constructor call with complete type information: CALL result = Type.&lt;init&gt;(args...)
   */
  public static CallInstr constructor(final String result,
                                      final DebugInfo debugInfo,
                                      final CallDetails callDetails) {
    return new CallInstr(IROpcode.CALL, result, debugInfo, callDetails);
  }

  private CallInstr(final IROpcode opcode,
                    final String result,
                    final DebugInfo debugInfo,
                    final CallDetails callDetails) {

    super(opcode, result, debugInfo);
    this.callDetails = callDetails;

    //This is just for human and debugging purposes.
    //When processing the IR, the actual methods on this class will be used.
    //There is no intention to serialise the operands to file to be processed.
    super.addOperand(callDetails.toString());
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  public String getTargetObject() {
    return callDetails.targetObject();
  }

  public String getMethodName() {
    return callDetails.methodName();
  }

  public List<String> getArguments() {
    return callDetails.arguments();
  }

  /**
   * Check if this is a constructor call.
   */
  public boolean isConstructorCall() {
    return "<init>".equals(callDetails.methodName());
  }

  /**
   * Get the EK9 format target type name (e.g., "org.ek9.lang::Stdout").
   */
  public String getTargetTypeName() {
    return callDetails.targetTypeName();
  }

  /**
   * Get parameter type names in EK9 format (e.g., ["org.ek9.lang::String"]).
   */
  public List<String> getParameterTypes() {
    return callDetails.parameterTypes();
  }

  /**
   * Get return type name in EK9 format (e.g., "org.ek9.lang::Void").
   */
  public String getReturnTypeName() {
    return callDetails.returnTypeName();
  }
}