package org.ek9lang.compiler.phase7.helpers;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;

/**
 * Handles IR generation for statements.
 *
 * <p>This helper consolidates statement generation that was previously
 * scattered across multiple generator classes. Uses stack-based context
 * to eliminate parameter threading.</p>
 */
public class StatementIRHelper extends AbstractIRHelper {

  public StatementIRHelper(IRInstructionBuilder instructionBuilder) {
    super(instructionBuilder);
  }

  /**
   * Generate IR for an instruction block.
   */
  public void generateFor(EK9Parser.InstructionBlockContext ctx) {
    //will generate
  }

  /**
   * Generate IR for an assignment statement.
   */
  public void generateAssignment(EK9Parser.AssignmentStatementContext ctx) {
    //will generate
  }

  /**
   * Generate IR for a block statement.
   */
  public void generateBlock(EK9Parser.BlockContext ctx) {
    //will generate
  }
}