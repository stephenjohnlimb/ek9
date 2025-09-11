package org.ek9lang.compiler.phase7.helpers;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;

/**
 * Handles IR generation for expressions.
 * 
 * <p>This helper consolidates expression generation that was previously
 * scattered across multiple generator classes. Uses stack-based context
 * to eliminate parameter threading.</p>
 */
public class ExpressionIRHelper extends AbstractIRHelper {

  public ExpressionIRHelper(IRInstructionBuilder instructionBuilder) {
    super(instructionBuilder);
  }

  /**
   * Generate IR for an expression.
   */
  public void generateFor(EK9Parser.ExpressionContext ctx) {
    //will generate.
  }
}