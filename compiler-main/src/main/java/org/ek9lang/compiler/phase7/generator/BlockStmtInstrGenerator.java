package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Creates IR instructions for block statements.
 * Handles variable declarations, variable only declarations, and statements.
 * <p>
 * Deals with the following ANTLR grammar.
 * </p>
 * <pre>
 *   blockStatement
 *     : variableDeclaration
 *     | variableOnlyDeclaration
 *     | statement
 *     ;
 * </pre>
 */
final class BlockStmtInstrGenerator extends AbstractGenerator {

  private final VariableDeclInstrGenerator variableDeclarationCreator;
  private final VariableOnlyDeclInstrGenerator variableOnlyDeclarationCreator;
  private final StmtInstrGenerator statementInstructionCreator;

  BlockStmtInstrGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
    this.variableDeclarationCreator = new VariableDeclInstrGenerator(stackContext);
    this.variableOnlyDeclarationCreator = new VariableOnlyDeclInstrGenerator(instructionBuilder);
    this.statementInstructionCreator = new StmtInstrGenerator(stackContext);
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