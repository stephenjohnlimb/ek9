package org.ek9lang.compiler.phase7;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for assignment expressions.
 * Generates new BasicBlock IR (IRInstructions).
 * <p>
 * Note that this is just really a 'pointer' assignment to some existing allocated object/memory.
 * It is not a deep copy in any way.
 * </p>
 * <p>
 * THis deals with the following ANTLR grammar.
 * </p>
 * <pre>
 *   assignmentExpression
 *     : expression
 *     | guardExpression
 *     | dynamicClassDeclaration
 *     | switchStatementExpression
 *     | tryStatementExpression
 *     | whileStatementExpression
 *     | forStatementExpression
 *     | streamExpression
 *     ;
 * </pre>
 */
final class AssignmentExprInstrGenerator implements
    Function<String, List<IRInstr>> {

  private final IRContext context;
  private final ExprInstrGenerator exprInstrGenerator;
  private final EK9Parser.AssignmentExpressionContext ctx;
  private final String scopeId;
  private final DebugInfoCreator debugInfoCreator;

  AssignmentExprInstrGenerator(final IRContext context,
                               final EK9Parser.AssignmentExpressionContext ctx,
                               final String scopeId) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    AssertValue.checkNotNull("AssignmentExpressionContext cannot be null", ctx);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);
    this.context = context;
    this.scopeId = scopeId;
    this.debugInfoCreator = new DebugInfoCreator(context);

    this.exprInstrGenerator = new ExprInstrGenerator(context);
    this.ctx = ctx;
  }

  /**
   * Generate IR instructions for assignment expression.
   */
  public List<IRInstr> apply(final String rhsExprResult) {

    final var debugInfo =
        debugInfoCreator.apply(context.getParsedModule().getRecordedSymbol(ctx.expression()).getSourceToken());
    final var exprDetails = new ExprProcessingDetails(ctx.expression(), rhsExprResult, scopeId, debugInfo);

    AssertValue.checkNotNull("RhsExprResult cannot be null", rhsExprResult);

    if (ctx.expression() != null) {
      return exprInstrGenerator.apply(exprDetails);
    } else if (ctx.guardExpression() != null) {
      AssertValue.fail("guardExpression not implemented");
    } else if (ctx.dynamicClassDeclaration() != null) {
      AssertValue.fail("dynamicClassDeclaration not implemented");
    } else if (ctx.switchStatementExpression() != null) {
      AssertValue.fail("switchStatementExpression not implemented");
    } else if (ctx.tryStatementExpression() != null) {
      AssertValue.fail("tryStatementExpression not implemented");
    } else if (ctx.whileStatementExpression() != null) {
      AssertValue.fail("whileStatementExpression not implemented");
    } else if (ctx.forStatementExpression() != null) {
      AssertValue.fail("forStatementExpression not implemented");
    } else if (ctx.streamExpression() != null) {
      AssertValue.fail("streamExpression not implemented");
    } else {
      AssertValue.fail("Expecting finite set of operations for assignment expression");
    }

    return List.of();
  }
}