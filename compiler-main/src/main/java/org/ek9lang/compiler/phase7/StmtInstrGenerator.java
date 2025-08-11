package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Creates IR instructions for statements.
 * Generates new BasicBlock IR (IRInstructions).
 * <p>From the ANTLR grammar this has to support the following:</p>
 * <pre>
 *   statement
 *     : ifStatement
 *     | assertStatement
 *     | assignmentStatement
 *     | identifierReference op=(INC | DEC)
 *     | call
 *     | throwStatement
 *     | objectAccessExpression
 *     | switchStatementExpression
 *     | tryStatementExpression
 *     | whileStatementExpression
 *     | forStatementExpression
 *     | streamStatement
 *     ;
 * </pre>
 */
final class StmtInstrGenerator implements BiFunction<EK9Parser.StatementContext, String, List<IRInstr>> {

  private final IRContext context;
  private final ObjectAccessInstrGenerator objectAccessGenerator;
  private final AssertStmtGenerator assertStmtGenerator;
  private final AssignmentStmtGenerator assignmentStmtGenerator;

  StmtInstrGenerator(final IRContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.objectAccessGenerator = new ObjectAccessInstrGenerator(context);
    this.assertStmtGenerator = new AssertStmtGenerator(context);
    this.assignmentStmtGenerator = new AssignmentStmtGenerator(context);
  }

  /**
   * Generate IR instructions for a statement.
   */
  public List<IRInstr> apply(final EK9Parser.StatementContext ctx, final String scopeId) {

    AssertValue.checkNotNull("StatementContext cannot be null", ctx);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    final var instructions = new ArrayList<IRInstr>();

    if (ctx.ifStatement() != null) {
      throw new CompilerException("If not implemented");
    } else if (ctx.assertStatement() != null) {
      processAssertStatement(ctx.assertStatement(), scopeId, instructions);
    } else if (ctx.assignmentStatement() != null) {
      processAssignmentStatement(ctx.assignmentStatement(), scopeId, instructions);
    } else if (ctx.identifierReference() != null) {
      throw new CompilerException("Identifier inc/dec not implemented");
    } else if (ctx.call() != null) {
      throw new CompilerException("Call not implemented");
    } else if (ctx.throwStatement() != null) {
      throw new CompilerException("Throw not implemented");
    } else if (ctx.objectAccessExpression() != null) {
      processObjectAccessExpression(ctx.objectAccessExpression(), scopeId, instructions);
    } else if (ctx.switchStatementExpression() != null) {
      throw new CompilerException("Switch not implemented");
    } else if (ctx.tryStatementExpression() != null) {
      throw new CompilerException("Try not implemented");
    } else if (ctx.whileStatementExpression() != null) {
      throw new CompilerException("While not implemented");
    } else if (ctx.forStatementExpression() != null) {
      throw new CompilerException("For not implemented");
    } else if (ctx.streamStatement() != null) {
      throw new CompilerException("Stream not implemented");
    } else {
      throw new CompilerException("Unexpected condition");
    }

    return instructions;
  }

  private void processObjectAccessExpression(final EK9Parser.ObjectAccessExpressionContext ctx,
                                             final String scopeId, final List<IRInstr> instructions) {

    final var tempResult = context.generateTempName();
    instructions.addAll(objectAccessGenerator.apply(ctx, tempResult, scopeId));

  }

  private void processAssertStatement(final EK9Parser.AssertStatementContext ctx,
                                      final String scopeId, final List<IRInstr> instructions) {

    instructions.addAll(assertStmtGenerator.apply(ctx, scopeId));

  }

  private void processAssignmentStatement(final EK9Parser.AssignmentStatementContext ctx,
                                          final String scopeId, final List<IRInstr> instructions) {

    instructions.addAll(assignmentStmtGenerator.apply(ctx, scopeId));

  }
}