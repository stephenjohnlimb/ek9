package org.ek9lang.compiler.phase7.support;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.calls.CallDetailsForOfFalse;

/**
 * Creates evaluation instructions for Boolean._ofFalse() static method call.
 * Generates a Boolean object with value false, including proper memory management.
 */
public final class BooleanFalseEvaluationCreator implements Function<VariableDetails, List<IRInstr>> {
  private final CallDetailsForOfFalse callDetailsForOfFalse;
  private final VariableMemoryManagement variableMemoryManagement;
  private final IRInstrToList irInstrToList = new IRInstrToList();

  public BooleanFalseEvaluationCreator(final CallDetailsForOfFalse callDetailsForOfFalse,
                                       final VariableMemoryManagement variableMemoryManagement) {
    this.callDetailsForOfFalse = callDetailsForOfFalse;
    this.variableMemoryManagement = variableMemoryManagement;
  }

  @Override
  public List<IRInstr> apply(final VariableDetails variableDetails) {
    final var instructions = irInstrToList
        .apply(() -> CallInstr.callStatic(variableDetails, callDetailsForOfFalse.get()));

    variableMemoryManagement.apply(() -> instructions, variableDetails);
    return instructions;
  }
}
