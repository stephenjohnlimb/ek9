package org.ek9lang.compiler.phase7.support;

import java.util.List;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.calls.BooleanNotCallDetailsCreator;

/**
 * Creates evaluation instructions for Boolean._not() method call.
 * Inverts a boolean value by calling the _not() operator (~).
 */
public final class BooleanNotEvaluationCreator {
  private final BooleanNotCallDetailsCreator booleanNotCallDetailsCreator;
  private final VariableMemoryManagement variableMemoryManagement;
  private final IRInstrToList irInstrToList = new IRInstrToList();

  public BooleanNotEvaluationCreator(final BooleanNotCallDetailsCreator booleanNotCallDetailsCreator,
                                     final VariableMemoryManagement variableMemoryManagement) {
    this.booleanNotCallDetailsCreator = booleanNotCallDetailsCreator;
    this.variableMemoryManagement = variableMemoryManagement;
  }

  /**
   * Creates Boolean._not() method call instructions.
   *
   * @param booleanVariable The boolean variable to invert
   * @param resultDetails   The result variable details
   * @return List of IR instructions for the _not() call
   */
  public List<IRInstr> apply(final String booleanVariable, final VariableDetails resultDetails) {
    final var notCallDetails = booleanNotCallDetailsCreator.apply(booleanVariable);
    final var instructions = irInstrToList.apply(() -> CallInstr.operator(resultDetails, notCallDetails));
    variableMemoryManagement.apply(() -> instructions, resultDetails);
    return instructions;
  }
}
