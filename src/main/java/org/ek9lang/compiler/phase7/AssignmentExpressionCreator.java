package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.INode;
import org.ek9lang.core.CompilerException;

/**
 * Processes the various ways of using assignment expressions.
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
public class AssignmentExpressionCreator implements Function<EK9Parser.AssignmentExpressionContext, INode> {

  private final ParsedModule parsedModule;
  private final ExpressionCreator expressionCreator;

  public AssignmentExpressionCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
    this.expressionCreator = new ExpressionCreator(parsedModule);

  }

  @Override
  public INode apply(final EK9Parser.AssignmentExpressionContext ctx) {
    if (ctx.expression() != null) {
      return expressionCreator.apply(ctx.expression());
    }
    throw new CompilerException("Assignment expression not fully implemented yet");
  }
}
