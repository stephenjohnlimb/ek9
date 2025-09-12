package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
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
 * <p>
 * MIGRATING TO STACK: Now uses stack context for scope management instead of parameter threading.
 * Still maintains Function interface for incremental migration approach.
 * </p>
 */
final class StmtInstrGenerator extends AbstractGenerator
    implements Function<EK9Parser.StatementContext, List<IRInstr>> {

  private final ObjectAccessInstrGenerator objectAccessGenerator;
  private final AssertStmtGenerator assertStmtGenerator;
  private final AssignmentStmtGenerator assignmentStmtGenerator;
  private final CallInstrGenerator callInstrGenerator;

  StmtInstrGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
    this.objectAccessGenerator = new ObjectAccessInstrGenerator(stackContext);
    this.assertStmtGenerator = new AssertStmtGenerator(stackContext);
    this.assignmentStmtGenerator = new AssignmentStmtGenerator(stackContext);
    this.callInstrGenerator = new CallInstrGenerator(stackContext);
  }

  /**
   * Generate IR instructions for a statement using proper stack-based scope management.
   * STACK-BASED: Gets scope ID from stack context instead of parameter threading.
   */
  @Override
  public List<IRInstr> apply(final EK9Parser.StatementContext ctx) {
    AssertValue.checkNotNull("StatementContext cannot be null", ctx);

    final var instructions = new ArrayList<IRInstr>();

    if (ctx.ifStatement() != null) {
      throw new CompilerException("If not implemented");
    } else if (ctx.assertStatement() != null) {
      processAssertStatement(ctx.assertStatement(), instructions);
    } else if (ctx.assignmentStatement() != null) {
      processAssignmentStatement(ctx.assignmentStatement(), instructions);
    } else if (ctx.identifierReference() != null) {
      throw new CompilerException("Identifier inc/dec not implemented");
    } else if (ctx.call() != null) {
      processCall(ctx.call(), instructions);
    } else if (ctx.throwStatement() != null) {
      throw new CompilerException("Throw not implemented");
    } else if (ctx.objectAccessExpression() != null) {
      processObjectAccessExpression(ctx.objectAccessExpression(), instructions);
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
                                             final List<IRInstr> instructions) {
    // STACK-BASED: Get scope ID and debug info from stack context
    final var scopeId = stackContext.currentScopeId();
    final var debugInfo = stackContext.createDebugInfo(ctx);
    final var tempResult = stackContext.generateTempName();
    final var variableDetails = new VariableDetails(tempResult, new BasicDetails(scopeId, debugInfo));
    instructions.addAll(objectAccessGenerator.apply(ctx, variableDetails));
  }

  private void processAssertStatement(final EK9Parser.AssertStatementContext ctx,
                                      final List<IRInstr> instructions) {
    // STACK-BASED: AssertStmtGenerator now uses stack context directly
    instructions.addAll(assertStmtGenerator.apply(ctx));
  }

  private void processAssignmentStatement(final EK9Parser.AssignmentStatementContext ctx,
                                          final List<IRInstr> instructions) {
    // STACK-BASED: AssignmentStmtGenerator now uses stack context directly
    instructions.addAll(assignmentStmtGenerator.apply(ctx));
  }

  private void processCall(final EK9Parser.CallContext ctx,
                           final List<IRInstr> instructions) {
    // STACK-BASED: Get scope ID and debug info from stack context
    final var scopeId = stackContext.currentScopeId();
    final var debugInfo = stackContext.createDebugInfo(ctx);
    final var tempResult = stackContext.generateTempName();
    final var variableDetails = new VariableDetails(tempResult, new BasicDetails(scopeId, debugInfo));
    instructions.addAll(callInstrGenerator.apply(ctx, variableDetails));
  }
}