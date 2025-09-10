package org.ek9lang.compiler.phase7;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.phase7.support.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRInstructionBuilder;

/**
 * Handles IR generation for expressions.
 * 
 * <p>This helper consolidates expression generation that was previously
 * scattered across multiple generator classes. Uses stack-based context
 * to eliminate parameter threading.</p>
 */
public class ExpressionIRHelper extends AbstractIRHelper {

  public ExpressionIRHelper(IRGenerationContext context, IRInstructionBuilder instructionBuilder) {
    super(context, instructionBuilder);
  }

  /**
   * Generate IR for an expression.
   */
  public void generateFor(EK9Parser.ExpressionContext ctx) {
    // TODO: Implement expression IR generation
    // This will consolidate logic from various expression generators
  }
}