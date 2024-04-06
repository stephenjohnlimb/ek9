package org.ek9lang.compiler.common;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;

/**
 * Just focussed on ensuring that scopes are correct push on to the scope stack.
 * The super deals with ensuring they are popped off the stack.
 */
public abstract class ScopeStackConsistencyListener extends AbstractEK9PhaseListener {
  protected ScopeStackConsistencyListener(final ParsedModule parsedModule) {

    super(parsedModule);

  }

  @Override
  public void enterPackageBlock(final EK9Parser.PackageBlockContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterPackageBlock(ctx);
  }

  @Override
  public void enterFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterFunctionDeclaration(ctx);
  }

  @Override
  public void enterRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterRecordDeclaration(ctx);
  }

  @Override
  public void enterTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterTraitDeclaration(ctx);
  }

  @Override
  public void enterClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterClassDeclaration(ctx);
  }

  @Override
  public void enterComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterComponentDeclaration(ctx);
  }

  @Override
  public void enterTextDeclaration(final EK9Parser.TextDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterTextDeclaration(ctx);
  }

  @Override
  public void enterTextBodyDeclaration(final EK9Parser.TextBodyDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterTextBodyDeclaration(ctx);
  }

  @Override
  public void enterServiceDeclaration(final EK9Parser.ServiceDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterServiceDeclaration(ctx);
  }

  @Override
  public void enterServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterServiceOperationDeclaration(ctx);
  }

  @Override
  public void enterApplicationDeclaration(final EK9Parser.ApplicationDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterApplicationDeclaration(ctx);
  }

  @Override
  public void enterDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterDynamicClassDeclaration(ctx);
  }

  @Override
  public void enterDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void enterDynamicVariableCapture(final EK9Parser.DynamicVariableCaptureContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterDynamicVariableCapture(ctx);
  }

  @Override
  public void enterMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    //This first scope will be the synthetic program
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    if (ctx.getParent() instanceof EK9Parser.ProgramBlockContext) {
      //There is a slight difference in scope stack management for programs.
      //This inner scope will actually be the main body
      var bodyScope = symbolAndScopeManagement.getRecordedScope(ctx.operationDetails());
      symbolAndScopeManagement.enterScope(bodyScope);
    }

    super.enterMethodDeclaration(ctx);
  }

  @Override
  public void enterOperatorDeclaration(final EK9Parser.OperatorDeclarationContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterOperatorDeclaration(ctx);
  }

  @Override
  public void enterTypeDeclaration(final EK9Parser.TypeDeclarationContext ctx) {

    if (ctx.Identifier() != null) {
      symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    }

    super.enterTypeDeclaration(ctx);
  }

  @Override
  public void enterIfStatement(final EK9Parser.IfStatementContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterIfStatement(ctx);
  }

  @Override
  public void enterSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterSwitchStatementExpression(ctx);
  }

  @Override
  public void enterForStatementExpression(final EK9Parser.ForStatementExpressionContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterForStatementExpression(ctx);
  }

  @Override
  public void enterWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterWhileStatementExpression(ctx);
  }

  @Override
  public void enterReturningParam(final EK9Parser.ReturningParamContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterReturningParam(ctx);
  }

  @Override
  public void enterTryStatementExpression(final EK9Parser.TryStatementExpressionContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterTryStatementExpression(ctx);
  }

  @Override
  public void enterCatchStatementExpression(final EK9Parser.CatchStatementExpressionContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterCatchStatementExpression(ctx);
  }

  @Override
  public void enterFinallyStatementExpression(final EK9Parser.FinallyStatementExpressionContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterFinallyStatementExpression(ctx);
  }

  @Override
  public void enterBlock(final EK9Parser.BlockContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterBlock(ctx);
  }

  @Override
  public void enterSingleStatementBlock(final EK9Parser.SingleStatementBlockContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterSingleStatementBlock(ctx);
  }

  @Override
  public void enterInstructionBlock(final EK9Parser.InstructionBlockContext ctx) {

    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));

    super.enterInstructionBlock(ctx);
  }
}
