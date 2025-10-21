package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.calls.CallDetailsBuilder;

/**
 * CONCERN: Unary operator invocation with memory management.
 * RESPONSIBILITY: Call unary operator (_inc, _dec, _neg, etc.) with ARC compliance.
 * REUSABILITY: ALL generators calling unary operators.
 * <p>
 * Encapsulates the pattern:
 * 1. Create CallContext for unary operation
 * 2. Build CallDetails via CallDetailsBuilder
 * 3. Execute operator call
 * 4. Apply memory management (RETAIN/SCOPE_REGISTER)
 * </p>
 * <p>
 * Ensures all unary operator calls follow consistent memory management,
 * preventing reference counting bugs.
 * </p>
 */
public final class UnaryOperatorInvoker implements Function<UnaryOperatorParams, List<IRInstr>> {
  private final CallDetailsBuilder callDetailsBuilder;
  private final VariableMemoryManagement variableMemoryManagement;

  public UnaryOperatorInvoker(
      final CallDetailsBuilder callDetailsBuilder,
      final VariableMemoryManagement variableMemoryManagement) {
    this.callDetailsBuilder = callDetailsBuilder;
    this.variableMemoryManagement = variableMemoryManagement;
  }

  /**
   * Invoke unary operator on operand.
   * <p>
   * Handles:
   * - CallContext creation
   * - CallDetailsBuilder invocation
   * - Memory management (RETAIN/SCOPE_REGISTER)
   * </p>
   *
   * @param params Unary operator parameters
   * @return Instructions ending with result in params.resultTemp()
   */
  @Override
  public List<IRInstr> apply(final UnaryOperatorParams params) {
    final var callContext = CallContext.forUnaryOperation(
        params.operandType(),
        params.operator(),
        params.operand(),
        params.resultType(),
        params.scopeId()
    );
    final var callResult = callDetailsBuilder.apply(callContext);

    final var callInstructions = new ArrayList<>(callResult.allInstructions());
    callInstructions.add(CallInstr.operator(
        params.resultTemp(),
        params.debugInfo(),
        callResult.callDetails()
    ));

    return variableMemoryManagement.apply(
        () -> callInstructions,
        new VariableDetails(params.resultTemp(), params.debugInfo())
    );
  }
}
