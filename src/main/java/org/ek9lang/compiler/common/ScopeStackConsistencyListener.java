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

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterPackageBlock(ctx);
  }

  @Override
  public void enterFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterFunctionDeclaration(ctx);
  }

  @Override
  public void enterRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterRecordDeclaration(ctx);
  }

  @Override
  public void enterTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterTraitDeclaration(ctx);
  }

  @Override
  public void enterClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterClassDeclaration(ctx);
  }

  @Override
  public void enterComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterComponentDeclaration(ctx);
  }

  @Override
  public void enterTextDeclaration(final EK9Parser.TextDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterTextDeclaration(ctx);
  }

  @Override
  public void enterTextBodyDeclaration(final EK9Parser.TextBodyDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterTextBodyDeclaration(ctx);
  }

  @Override
  public void enterServiceDeclaration(final EK9Parser.ServiceDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterServiceDeclaration(ctx);
  }

  @Override
  public void enterServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterServiceOperationDeclaration(ctx);
  }

  @Override
  public void enterApplicationDeclaration(final EK9Parser.ApplicationDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterApplicationDeclaration(ctx);
  }

  @Override
  public void enterDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterDynamicClassDeclaration(ctx);
  }

  @Override
  public void enterDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void enterDynamicVariableCapture(final EK9Parser.DynamicVariableCaptureContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterDynamicVariableCapture(ctx);
  }

  @Override
  public void enterMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    //This first scope will be the synthetic program
    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));
    if (ctx.getParent() instanceof EK9Parser.ProgramBlockContext) {
      //There is a slight difference in scope stack management for programs.
      //This inner scope will actually be the main body
      var bodyScope = symbolsAndScopes.getRecordedScope(ctx.operationDetails());
      symbolsAndScopes.enterScope(bodyScope);
    }

    super.enterMethodDeclaration(ctx);
  }

  @Override
  public void enterOperatorDeclaration(final EK9Parser.OperatorDeclarationContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterOperatorDeclaration(ctx);
  }

  @Override
  public void enterTypeDeclaration(final EK9Parser.TypeDeclarationContext ctx) {

    if (ctx.Identifier() != null) {
      symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));
    }

    super.enterTypeDeclaration(ctx);
  }

  @Override
  public void enterIfStatement(final EK9Parser.IfStatementContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterIfStatement(ctx);
  }

  @Override
  public void enterSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterSwitchStatementExpression(ctx);
  }

  @Override
  public void enterForStatementExpression(final EK9Parser.ForStatementExpressionContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterForStatementExpression(ctx);
  }

  @Override
  public void enterWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterWhileStatementExpression(ctx);
  }

  @Override
  public void enterReturningParam(final EK9Parser.ReturningParamContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterReturningParam(ctx);
  }

  @Override
  public void enterTryStatementExpression(final EK9Parser.TryStatementExpressionContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterTryStatementExpression(ctx);
  }

  @Override
  public void enterCatchStatementExpression(final EK9Parser.CatchStatementExpressionContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterCatchStatementExpression(ctx);
  }

  @Override
  public void enterFinallyStatementExpression(final EK9Parser.FinallyStatementExpressionContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterFinallyStatementExpression(ctx);
  }

  @Override
  public void enterBlock(final EK9Parser.BlockContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterBlock(ctx);
  }

  @Override
  public void enterSingleStatementBlock(final EK9Parser.SingleStatementBlockContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterSingleStatementBlock(ctx);
  }

  @Override
  public void enterInstructionBlock(final EK9Parser.InstructionBlockContext ctx) {

    symbolsAndScopes.enterScope(symbolsAndScopes.getRecordedScope(ctx));

    super.enterInstructionBlock(ctx);
  }
}
