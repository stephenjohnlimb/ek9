package org.ek9lang.compiler.phase5;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ScopeStackConsistencyListener;

/**
 * Designed to do additional logic checks once everything has been resolved.
 * Initially focused on ensuring that uninitialised variables are initialised before being used.
 * But may extend to other forms of checks. For example booleans set to literals and never altered and used in an 'if'.
 */
final class PreIRCheckListener extends ScopeStackConsistencyListener {
  private final ErrorListener errorListener;
  private final ProcessVariableOnlyDeclaration processVariableOnlyDeclaration;
  private final ProcessAssignmentStatement processAssignmentStatement;
  private final ProcessGuardExpression processGuardExpression;
  private final ProcessIdentifierReference processIdentifierReference;
  private final ProcessIfStatement processIfStatement;
  private final ProcessFunctionDeclaration processFunctionDeclaration;
  private final ProcessMethodDeclaration processMethodDeclaration;
  private final ProcessOperatorDeclaration processOperatorDeclaration;
  private final ProcessServiceOperationDeclaration processServiceOperationDeclaration;
  private final ProcessDynamicFunctionDeclarationEntry processDynamicFunctionDeclarationEntry;
  private final ProcessDynamicFunctionDeclarationExit processDynamicFunctionDeclarationExit;

  PreIRCheckListener(ParsedModule parsedModule) {
    super(parsedModule);
    this.errorListener = parsedModule.getSource().getErrorListener();
    this.processVariableOnlyDeclaration =
        new ProcessVariableOnlyDeclaration(symbolAndScopeManagement, errorListener);
    this.processAssignmentStatement =
        new ProcessAssignmentStatement(symbolAndScopeManagement, errorListener);
    this.processGuardExpression =
        new ProcessGuardExpression(symbolAndScopeManagement, errorListener);
    this.processIdentifierReference =
        new ProcessIdentifierReference(symbolAndScopeManagement, errorListener);
    this.processIfStatement =
        new ProcessIfStatement(symbolAndScopeManagement, errorListener);
    this.processFunctionDeclaration =
        new ProcessFunctionDeclaration(symbolAndScopeManagement, errorListener);
    this.processMethodDeclaration =
        new ProcessMethodDeclaration(symbolAndScopeManagement, errorListener);
    this.processOperatorDeclaration =
        new ProcessOperatorDeclaration(symbolAndScopeManagement, errorListener);
    this.processServiceOperationDeclaration =
        new ProcessServiceOperationDeclaration(symbolAndScopeManagement, errorListener);
    this.processDynamicFunctionDeclarationEntry =
        new ProcessDynamicFunctionDeclarationEntry(symbolAndScopeManagement, errorListener);
    this.processDynamicFunctionDeclarationExit =
        new ProcessDynamicFunctionDeclarationExit(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void enterVariableOnlyDeclaration(EK9Parser.VariableOnlyDeclarationContext ctx) {
    processVariableOnlyDeclaration.accept(ctx);
    super.enterVariableOnlyDeclaration(ctx);
  }

  @Override
  public void enterAssignmentStatement(EK9Parser.AssignmentStatementContext ctx) {
    processAssignmentStatement.accept(ctx);
    super.enterAssignmentStatement(ctx);
  }

  @Override
  public void enterGuardExpression(EK9Parser.GuardExpressionContext ctx) {
    processGuardExpression.accept(ctx);
    super.enterGuardExpression(ctx);
  }

  @Override
  public void exitIdentifierReference(EK9Parser.IdentifierReferenceContext ctx) {
    processIdentifierReference.accept(ctx);
    super.exitIdentifierReference(ctx);
  }

  @Override
  public void exitIfStatement(EK9Parser.IfStatementContext ctx) {
    //Note that exit to pop stack first.
    super.exitIfStatement(ctx);
    processIfStatement.accept(ctx);
  }

  @Override
  public void exitSwitchStatementExpression(EK9Parser.SwitchStatementExpressionContext ctx) {
    //TODO need to look at preflow guard - if it was used and check if variable was set in there.

    //TODO Also need to check every case and default for variables to ensure initialised (if every path is)
    super.exitSwitchStatementExpression(ctx);
  }

  @Override
  public void exitTryStatementExpression(EK9Parser.TryStatementExpressionContext ctx) {
    //This is for each variable at this scope, once we know it is initialised in preflow all good.
    //So this below is a cascade of more checks but once we know it's assigned that's it.
    //TODO need to look at preflow guard - if it was used and check if variable was set in there.
    //TODO then check the finally
    //TODO need to check main try body
    //TODO check the catch body


    super.exitTryStatementExpression(ctx);

  }

  /**
   * On enter dynamic function need to record any return symbol, because we won't parse the text as it is inferred.
   * The on exit still need to do the same return processing to see if the rtn has been initialised.
   * But again cannot depend on source structure because the return is inferred and also the error has to
   * appear on the dynamic function declaration because there will be not '&lt;-' to report the error on.
   * Downside of having dynamic function infer arguments and returns, but worth it for the terseness.
   */
  @Override
  public void enterDynamicFunctionDeclaration(EK9Parser.DynamicFunctionDeclarationContext ctx) {
    processDynamicFunctionDeclarationEntry.accept(ctx);
    super.enterDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void exitDynamicFunctionDeclaration(EK9Parser.DynamicFunctionDeclarationContext ctx) {
    processDynamicFunctionDeclarationExit.accept(ctx);

    super.exitDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void exitFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    processFunctionDeclaration.accept(ctx);
    super.exitFunctionDeclaration(ctx);
  }

  @Override
  public void exitMethodDeclaration(EK9Parser.MethodDeclarationContext ctx) {
    processMethodDeclaration.accept(ctx);
    super.exitMethodDeclaration(ctx);
  }

  @Override
  public void exitOperatorDeclaration(EK9Parser.OperatorDeclarationContext ctx) {
    processOperatorDeclaration.accept(ctx);
    super.exitOperatorDeclaration(ctx);
  }

  @Override
  public void exitServiceOperationDeclaration(EK9Parser.ServiceOperationDeclarationContext ctx) {
    processServiceOperationDeclaration.accept(ctx);
    super.exitServiceOperationDeclaration(ctx);
  }
}
