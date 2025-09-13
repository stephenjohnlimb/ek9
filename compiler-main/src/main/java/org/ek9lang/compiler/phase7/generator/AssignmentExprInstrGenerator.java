package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
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
final class AssignmentExprInstrGenerator extends AbstractGenerator
    implements Function<String, List<IRInstr>> {

  private final ExprInstrGenerator exprInstrGenerator;
  private final EK9Parser.AssignmentExpressionContext ctx;

  AssignmentExprInstrGenerator(final IRGenerationContext stackContext,
                               final EK9Parser.AssignmentExpressionContext ctx) {
    super(stackContext);
    AssertValue.checkNotNull("AssignmentExpressionContext cannot be null", ctx);

    this.exprInstrGenerator = new ExprInstrGenerator(stackContext);
    this.ctx = ctx;
  }

  /**
   * Generate IR instructions for assignment expression using stack-based scope management.
   * STACK-BASED: Gets scope ID from stack context instead of constructor parameter.
   */
  public List<IRInstr> apply(final String rhsExprResult) {

    final var debugInfo =
        debugInfoCreator.apply(getRecordedSymbolOrException(ctx.expression()).getSourceToken());
    final var exprDetails = new ExprProcessingDetails(ctx.expression(),
        new VariableDetails(rhsExprResult, debugInfo));

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