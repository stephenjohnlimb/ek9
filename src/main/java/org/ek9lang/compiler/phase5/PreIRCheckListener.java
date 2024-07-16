package org.ek9lang.compiler.phase5;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ScopeStackConsistencyListener;

/**
 * Designed to do additional logic checks once everything has been resolved.
 * Initially focused on ensuring that uninitialised variables are initialised before being used.
 * But may extend to other forms of checks. For example booleans set to literals and never altered and used in an 'if'.
 */
final class PreIRCheckListener extends ScopeStackConsistencyListener {
  private final ProcessVariableOnlyDeclaration processVariableOnlyDeclaration;
  private final ProcessAssignmentStatement processAssignmentStatement;
  private final ProcessGuardExpression processGuardExpression;
  private final ProcessIdentifierReference processIdentifierReference;
  private final ProcessIfStatement processIfStatement;
  private final ProcessSwitchStatement processSwitchStatement;
  private final ProcessTryStatement processTryStatement;
  private final ProcessForStatement processForStatement;
  private final ProcessWhileStatement processWhileStatement;
  private final ProcessFunctionDeclaration processFunctionDeclaration;
  private final ProcessMethodDeclaration processMethodDeclaration;
  private final ProcessOperatorDeclaration processOperatorDeclaration;
  private final ProcessServiceOperationDeclaration processServiceOperationDeclaration;
  private final ProcessDynamicFunctionDeclarationEntry processDynamicFunctionDeclarationEntry;
  private final ProcessDynamicFunctionDeclarationExit processDynamicFunctionDeclarationExit;
  private final ProcessIdentifierAsProperty processIdentifierAsProperty;

  PreIRCheckListener(final ParsedModule parsedModule) {
    
    super(parsedModule);
    final var errorListener = parsedModule.getSource().getErrorListener();
    
    this.processVariableOnlyDeclaration =
        new ProcessVariableOnlyDeclaration(symbolsAndScopes, errorListener);
    this.processAssignmentStatement =
        new ProcessAssignmentStatement(symbolsAndScopes, errorListener);
    this.processGuardExpression =
        new ProcessGuardExpression(symbolsAndScopes, errorListener);
    this.processIdentifierReference =
        new ProcessIdentifierReference(symbolsAndScopes, errorListener);
    this.processIfStatement =
        new ProcessIfStatement(symbolsAndScopes, errorListener);
    this.processSwitchStatement =
        new ProcessSwitchStatement(symbolsAndScopes, errorListener);
    this.processForStatement =
        new ProcessForStatement(symbolsAndScopes, errorListener);
    this.processTryStatement =
        new ProcessTryStatement(symbolsAndScopes, errorListener);
    this.processWhileStatement =
        new ProcessWhileStatement(symbolsAndScopes, errorListener);
    this.processFunctionDeclaration =
        new ProcessFunctionDeclaration(symbolsAndScopes, errorListener);
    this.processMethodDeclaration =
        new ProcessMethodDeclaration(symbolsAndScopes, errorListener);
    this.processOperatorDeclaration =
        new ProcessOperatorDeclaration(symbolsAndScopes, errorListener);
    this.processServiceOperationDeclaration =
        new ProcessServiceOperationDeclaration(symbolsAndScopes, errorListener);
    this.processDynamicFunctionDeclarationEntry =
        new ProcessDynamicFunctionDeclarationEntry(symbolsAndScopes, errorListener);
    this.processDynamicFunctionDeclarationExit =
        new ProcessDynamicFunctionDeclarationExit(symbolsAndScopes, errorListener);
    this.processIdentifierAsProperty =
        new ProcessIdentifierAsProperty(symbolsAndScopes, errorListener);
    
  }

  @Override
  public void enterVariableOnlyDeclaration(final EK9Parser.VariableOnlyDeclarationContext ctx) {

    processVariableOnlyDeclaration.accept(ctx);

    super.enterVariableOnlyDeclaration(ctx);
  }

  @Override
  public void enterAssignmentStatement(final EK9Parser.AssignmentStatementContext ctx) {

    processAssignmentStatement.accept(ctx);

    super.enterAssignmentStatement(ctx);
  }

  @Override
  public void enterGuardExpression(final EK9Parser.GuardExpressionContext ctx) {

    processGuardExpression.accept(ctx);

    super.enterGuardExpression(ctx);
  }

  @Override
  public void enterIdentifier(final EK9Parser.IdentifierContext ctx) {

    //If may or may not be an aggregate property but this consumer will determine that.
    processIdentifierAsProperty.accept(ctx);

    super.enterIdentifier(ctx);
  }

  @Override
  public void exitIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {

    processIdentifierReference.accept(ctx);

    super.exitIdentifierReference(ctx);
  }

  @Override
  public void exitIfStatement(final EK9Parser.IfStatementContext ctx) {

    //Note that exit to pop stack first.
    super.exitIfStatement(ctx);

    processIfStatement.accept(ctx);
  }

  @Override
  public void exitSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {

    super.exitSwitchStatementExpression(ctx);

    processSwitchStatement.accept(ctx);
  }

  @Override
  public void exitTryStatementExpression(final EK9Parser.TryStatementExpressionContext ctx) {

    super.exitTryStatementExpression(ctx);

    processTryStatement.accept(ctx);
  }

  @Override
  public void exitForStatementExpression(final EK9Parser.ForStatementExpressionContext ctx) {

    super.exitForStatementExpression(ctx);

    processForStatement.accept(ctx);
  }

  @Override
  public void exitWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {

    super.exitWhileStatementExpression(ctx);

    processWhileStatement.accept(ctx);
  }

  /**
   * On enter dynamic function need to record any return symbol, because we won't parse the text as it is inferred.
   * The on exit still need to do the same return processing to see if the rtn has been initialised.
   * But again cannot depend on source structure because the return is inferred and also the error has to
   * appear on the dynamic function declaration because there will be not '&lt;-' to report the error on.
   * Downside of having dynamic function infer arguments and returns, but worth it for the terseness.
   */
  @Override
  public void enterDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    processDynamicFunctionDeclarationEntry.accept(ctx);

    super.enterDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void exitDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    processDynamicFunctionDeclarationExit.accept(ctx);

    super.exitDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void exitFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    processFunctionDeclaration.accept(ctx);

    super.exitFunctionDeclaration(ctx);
  }

  @Override
  public void exitMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    processMethodDeclaration.accept(ctx);

    super.exitMethodDeclaration(ctx);
  }

  @Override
  public void exitOperatorDeclaration(final EK9Parser.OperatorDeclarationContext ctx) {

    processOperatorDeclaration.accept(ctx);

    super.exitOperatorDeclaration(ctx);
  }

  @Override
  public void exitServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    processServiceOperationDeclaration.accept(ctx);

    super.exitServiceOperationDeclaration(ctx);
  }

}
