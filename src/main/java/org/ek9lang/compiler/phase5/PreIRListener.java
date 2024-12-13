package org.ek9lang.compiler.phase5;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ScopeStackConsistencyListener;

/**
 * Designed to do additional logic checks once everything has been resolved.
 * Initially focused on ensuring that uninitialised variables are initialised before being used.
 * But may extend to other forms of checks. For example booleans set to literals and never altered and used in an 'if'.
 * This is designed to try and catch as much as possible before creating an Intermediate Representation and doing
 * further checks.
 */
final class PreIRListener extends ScopeStackConsistencyListener {
  private final VariableOnlyOrError variableOnlyOrError;
  private final AssignmentStatementOrError assignmentStatementOrError;
  private final ProcessGuardExpression processGuardExpression;
  private final IdentifierReferenceOrError identifierReferenceOrError;
  private final IfStatementOrError ifStatementOrError;
  private final SwitchStatementOrError switchStatementOrError;
  private final TryStatementOrError tryStatementOrError;
  private final ForStatementOrError forStatementOrError;
  private final WhileStatementOrError whileStatementOrError;
  private final FunctionOrError functionOrError;
  private final MethodOrError methodOrError;
  private final OperatorOrError operatorOrError;
  private final ServiceOperationOrError serviceOperationOrError;
  private final ProcessDynamicFunctionEntry processDynamicFunctionDeclarationEntry;
  private final DynamicFunctionOrError dynamicFunctionOrError;
  private final IdentifierAsPropertyOrError processIdentifierAsProperty;

  PreIRListener(final ParsedModule parsedModule) {

    super(parsedModule);
    final var errorListener = parsedModule.getSource().getErrorListener();

    this.variableOnlyOrError =
        new VariableOnlyOrError(symbolsAndScopes, errorListener);
    this.assignmentStatementOrError =
        new AssignmentStatementOrError(symbolsAndScopes, errorListener);
    this.processGuardExpression =
        new ProcessGuardExpression(symbolsAndScopes, errorListener);
    this.identifierReferenceOrError =
        new IdentifierReferenceOrError(symbolsAndScopes, errorListener);
    this.ifStatementOrError =
        new IfStatementOrError(symbolsAndScopes, errorListener);
    this.switchStatementOrError =
        new SwitchStatementOrError(symbolsAndScopes, errorListener);
    this.forStatementOrError =
        new ForStatementOrError(symbolsAndScopes, errorListener);
    this.tryStatementOrError =
        new TryStatementOrError(symbolsAndScopes, errorListener);
    this.whileStatementOrError =
        new WhileStatementOrError(symbolsAndScopes, errorListener);
    this.functionOrError =
        new FunctionOrError(symbolsAndScopes, errorListener);
    this.methodOrError =
        new MethodOrError(symbolsAndScopes, errorListener);
    this.operatorOrError =
        new OperatorOrError(symbolsAndScopes, errorListener);
    this.serviceOperationOrError =
        new ServiceOperationOrError(symbolsAndScopes, errorListener);
    this.processDynamicFunctionDeclarationEntry =
        new ProcessDynamicFunctionEntry(symbolsAndScopes, errorListener);
    this.dynamicFunctionOrError =
        new DynamicFunctionOrError(symbolsAndScopes, errorListener);
    this.processIdentifierAsProperty =
        new IdentifierAsPropertyOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void enterVariableOnlyDeclaration(final EK9Parser.VariableOnlyDeclarationContext ctx) {

    variableOnlyOrError.accept(ctx);

    super.enterVariableOnlyDeclaration(ctx);
  }

  @Override
  public void enterAssignmentStatement(final EK9Parser.AssignmentStatementContext ctx) {

    assignmentStatementOrError.accept(ctx);

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

    identifierReferenceOrError.accept(ctx);

    super.exitIdentifierReference(ctx);
  }

  @Override
  public void exitIfStatement(final EK9Parser.IfStatementContext ctx) {

    //Note that exit to pop stack first.
    super.exitIfStatement(ctx);

    ifStatementOrError.accept(ctx);
  }

  @Override
  public void exitSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {

    super.exitSwitchStatementExpression(ctx);

    switchStatementOrError.accept(ctx);
  }

  @Override
  public void exitTryStatementExpression(final EK9Parser.TryStatementExpressionContext ctx) {

    super.exitTryStatementExpression(ctx);

    tryStatementOrError.accept(ctx);
  }

  @Override
  public void exitForStatementExpression(final EK9Parser.ForStatementExpressionContext ctx) {

    super.exitForStatementExpression(ctx);

    forStatementOrError.accept(ctx);
  }

  @Override
  public void exitWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {

    super.exitWhileStatementExpression(ctx);

    whileStatementOrError.accept(ctx);
  }

  /**
   * On entry of a dynamic function need to record any return symbol, because we won't parse the text as it is inferred.
   * The on exit handler still needs to do the same return processing to see if the rtn has been initialised.
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

    dynamicFunctionOrError.accept(ctx);

    super.exitDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void exitFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    functionOrError.accept(ctx);

    super.exitFunctionDeclaration(ctx);
  }

  @Override
  public void exitMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    methodOrError.accept(ctx);

    super.exitMethodDeclaration(ctx);
  }

  @Override
  public void exitOperatorDeclaration(final EK9Parser.OperatorDeclarationContext ctx) {

    operatorOrError.accept(ctx);

    super.exitOperatorDeclaration(ctx);
  }

  @Override
  public void exitServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    serviceOperationOrError.accept(ctx);

    super.exitServiceOperationDeclaration(ctx);
  }

}
