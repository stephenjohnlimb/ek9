package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.calls.CallDetailsBuilder;

/**
 * CONCERN: Binary operator invocation with memory management.
 * RESPONSIBILITY: Call binary operator (_add, _lt, _cmp, etc.) with ARC compliance.
 * REUSABILITY: ALL generators calling binary operators.
 * <p>
 * Encapsulates the pattern:
 * 1. Create CallContext for binary operation
 * 2. Build CallDetails via CallDetailsBuilder
 * 3. Execute operator call
 * 4. Apply memory management (RETAIN/SCOPE_REGISTER)
 * </p>
 * <p>
 * Ensures all binary operator calls follow consistent memory management,
 * preventing reference counting bugs.
 * </p>
 */
public final class BinaryOperatorInvoker implements Function<BinaryOperatorParams, List<IRInstr>> {
  private final CallDetailsBuilder callDetailsBuilder;
  private final VariableMemoryManagement variableMemoryManagement;

  public BinaryOperatorInvoker(
      final CallDetailsBuilder callDetailsBuilder,
      final VariableMemoryManagement variableMemoryManagement) {
    this.callDetailsBuilder = callDetailsBuilder;
    this.variableMemoryManagement = variableMemoryManagement;
  }

  /**
   * Invoke binary operator on two operands.
   * <p>
   * Handles:
   * - CallContext creation
   * - CallDetailsBuilder invocation
   * - Memory management (RETAIN/SCOPE_REGISTER) for operators with return values
   * </p>
   * <p>
   * For void-returning operators (+=, -=, *=, /=), resultTemp may be null.
   * These mutating operators modify the left operand in place and don't return a value.
   * </p>
   *
   * @param params Binary operator parameters
   * @return Instructions ending with result in params.resultTemp() (if non-void)
   */
  @Override
  public List<IRInstr> apply(final BinaryOperatorParams params) {
    final var callContext = CallContext.forBinaryOperation(
        params.leftType(),
        params.rightType(),
        params.resultType(),
        params.operator(),
        params.leftOperand(),
        params.rightOperand(),
        params.scopeId()
    );
    final var callResult = callDetailsBuilder.apply(callContext);

    final var callInstructions = new ArrayList<>(callResult.allInstructions());
    callInstructions.add(CallInstr.operator(
        params.resultTemp(),
        params.debugInfo(),
        callResult.callDetails()
    ));

    // For void-returning operators (mutating operators like +=), skip memory management
    // as there is no result to manage
    if (params.resultTemp() == null) {
      return callInstructions;
    }

    return variableMemoryManagement.apply(
        () -> callInstructions,
        new VariableDetails(params.resultTemp(), params.debugInfo())
    );
  }
}
