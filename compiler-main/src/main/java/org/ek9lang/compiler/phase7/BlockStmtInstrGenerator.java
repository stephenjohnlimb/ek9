package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Creates IR instructions for block statements.
 * Handles variable declarations, variable only declarations, and statements.
 * <p>
 *   Deals with the following ANLR grammar.
 * </p>
 * <pre>
 *   blockStatement
 *     : variableDeclaration
 *     | variableOnlyDeclaration
 *     | statement
 *     ;
 * </pre>
 */
final class BlockStmtInstrGenerator {

  private final VariableDeclInstrGenerator variableDeclarationCreator;
  private final VariableOnlyDeclInstrGenerator variableOnlyDeclarationCreator;
  private final StmtInstrGenerator statementInstructionCreator;

  BlockStmtInstrGenerator(final IRContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.variableDeclarationCreator = new VariableDeclInstrGenerator(context);
    this.variableOnlyDeclarationCreator = new VariableOnlyDeclInstrGenerator(context);
    this.statementInstructionCreator = new StmtInstrGenerator(context);
  }

  /**
   * Generate IR instructions for a block statement.
   */
  public List<IRInstr> apply(final EK9Parser.BlockStatementContext ctx, final String scopeId) {
    AssertValue.checkNotNull("BlockStatementContext cannot be null", ctx);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    final var instructions = new ArrayList<IRInstr>();

    if (ctx.variableDeclaration() != null) {
      instructions.addAll(variableDeclarationCreator.apply(ctx.variableDeclaration(), scopeId));
    } else if (ctx.variableOnlyDeclaration() != null) {
      instructions.addAll(variableOnlyDeclarationCreator.apply(ctx.variableOnlyDeclaration(), scopeId));
    } else if (ctx.statement() != null) {
      instructions.addAll(statementInstructionCreator.apply(ctx.statement(), scopeId));
    } else {
      throw new CompilerException("Not expecting any other type of block statement");
    }

    return instructions;
  }
}