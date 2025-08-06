package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstruction;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for assignment expressions.
 * Generates new BasicBlock IR (IRInstructions) instead of old Block IR (INode).
 */
public final class AssignmentExpressionInstructionCreator {

  private final IRGenerationContext context;
  private final ExpressionInstructionCreator expressionCreator;

  public AssignmentExpressionInstructionCreator(final IRGenerationContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.expressionCreator = new ExpressionInstructionCreator(context);
  }

  /**
   * Generate IR instructions for assignment expression.
   */
  public List<IRInstruction> apply(final EK9Parser.AssignmentExpressionContext ctx, 
                                   final String resultVar, 
                                   final String scopeId) {
    AssertValue.checkNotNull("AssignmentExpressionContext cannot be null", ctx);
    AssertValue.checkNotNull("resultVar cannot be null", resultVar);  
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);
    
    final var instructions = new ArrayList<IRInstruction>();
    
    // Navigate through the assignment expression to find the actual expression
    if (ctx.expression() != null) {
      instructions.addAll(expressionCreator.apply(ctx.expression(), resultVar, scopeId));
    }
    
    return instructions;
  }
}