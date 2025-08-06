package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstruction;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for block statements.
 * Handles variable declarations, variable only declarations, and statements.
 * Generates new BasicBlock IR (IRInstructions) instead of old Block IR (INode).
 */
public final class BlockStatementInstructionCreator {

  private final IRGenerationContext context;
  private final VariableDeclarationInstructionCreator variableDeclarationCreator;
  private final StatementInstructionCreator statementInstructionCreator;

  public BlockStatementInstructionCreator(final IRGenerationContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.variableDeclarationCreator = new VariableDeclarationInstructionCreator(context);
    this.statementInstructionCreator = new StatementInstructionCreator(context);
  }

  /**
   * Generate IR instructions for a block statement.
   */
  public List<IRInstruction> apply(final EK9Parser.BlockStatementContext ctx, final String scopeId) {
    AssertValue.checkNotNull("BlockStatementContext cannot be null", ctx);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);
    
    final var instructions = new ArrayList<IRInstruction>();
    
    if (ctx.variableDeclaration() != null) {
      instructions.addAll(variableDeclarationCreator.apply(ctx.variableDeclaration(), scopeId));
    } else if (ctx.variableOnlyDeclaration() != null) {
      instructions.addAll(variableDeclarationCreator.applyVariableOnly(ctx.variableOnlyDeclaration(), scopeId));
    } else if (ctx.statement() != null) {
      instructions.addAll(statementInstructionCreator.apply(ctx.statement(), scopeId));
    }
    
    return instructions;
  }
}