package org.ek9lang.compiler.phase5;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ScopeStackConsistencyListener;

/**
 * Designed to do additional logic checks once everything has been resolved.
 */
public class PostSymbolResolutionListener extends ScopeStackConsistencyListener {
  protected final ErrorListener errorListener;

  protected PostSymbolResolutionListener(ParsedModule parsedModule) {
    super(parsedModule);
    this.errorListener = parsedModule.getSource().getErrorListener();
  }

  @Override
  public void exitIdentifierReference(EK9Parser.IdentifierReferenceContext ctx) {
    //System.out.println("Deal with identifier references");
    super.exitIdentifierReference(ctx);
  }

  @Override
  public void exitGuardExpression(EK9Parser.GuardExpressionContext ctx) {
    //System.out.println("Deal with guard assignments");
    super.exitGuardExpression(ctx);
  }

  @Override
  public void exitAssignmentStatement(EK9Parser.AssignmentStatementContext ctx) {
    //System.out.println("Deal with assignment statement");
    super.exitAssignmentStatement(ctx);
  }
}
