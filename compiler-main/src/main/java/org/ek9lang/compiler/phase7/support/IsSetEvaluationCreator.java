package org.ek9lang.compiler.phase7.support;

import java.util.List;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.calls.IsSetCallDetailsCreator;

/**
 * Creates evaluation instructions for _isSet() method calls.
 * Only creates the _isSet() call and result memory management.
 * Caller is responsible for loading the operand if needed.
 */
public final class IsSetEvaluationCreator {
  private final IsSetCallDetailsCreator isSetCallDetailsCreator;
  private final VariableMemoryManagement variableMemoryManagement;
  private final IRInstrToList irInstrToList = new IRInstrToList();

  public IsSetEvaluationCreator(final IsSetCallDetailsCreator isSetCallDetailsCreator,
                                final VariableMemoryManagement variableMemoryManagement) {
    this.isSetCallDetailsCreator = isSetCallDetailsCreator;
    this.variableMemoryManagement = variableMemoryManagement;
  }

  /**
   * Creates _isSet() method call instructions.
   *
   * @param operandVariable The variable to call _isSet() on
   * @param operandType     The type of the operand
   * @param resultDetails   The result variable details
   * @return List of IR instructions for the _isSet() call
   */
  public List<IRInstr> apply(final String operandVariable,
                             final String operandType,
                             final VariableDetails resultDetails) {
    final var isSetCallDetails = isSetCallDetailsCreator.apply(operandVariable, operandType);
    final var instructions = irInstrToList.apply(() -> CallInstr.operator(resultDetails, isSetCallDetails));
    variableMemoryManagement.apply(() -> instructions, resultDetails);
    return instructions;
  }
}
