package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstruction;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for statements.
 * Generates new BasicBlock IR (IRInstructions) instead of old Block IR (INode).
 */
public final class StatementInstructionCreator {

  private final IRGenerationContext context;
  private final ObjectAccessInstructionCreator objectAccessCreator;

  public StatementInstructionCreator(final IRGenerationContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.objectAccessCreator = new ObjectAccessInstructionCreator(context);
  }

  /**
   * Generate IR instructions for a statement.
   */
  public List<IRInstruction> apply(final EK9Parser.StatementContext ctx, final String scopeId) {
    AssertValue.checkNotNull("StatementContext cannot be null", ctx);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);
    
    final var instructions = new ArrayList<IRInstruction>();
    
    if (ctx.objectAccessExpression() != null) {
      final var tempResult = context.generateTempName();
      instructions.addAll(objectAccessCreator.apply(ctx.objectAccessExpression(), tempResult, scopeId));
    }
    
    return instructions;
  }
}