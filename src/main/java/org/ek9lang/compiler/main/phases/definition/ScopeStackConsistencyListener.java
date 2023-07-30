package org.ek9lang.compiler.main.phases.definition;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.main.phases.common.AbstractEK9PhaseListener;

/**
 * Just focussed on ensuring that scopes are correct push on to the scope stack.
 * The super deals with ensuring they are popped off the stack.
 */
public abstract class ScopeStackConsistencyListener extends AbstractEK9PhaseListener {
  protected ScopeStackConsistencyListener(ParsedModule parsedModule) {
    super(parsedModule);
  }

  @Override
  public void enterPackageBlock(EK9Parser.PackageBlockContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterPackageBlock(ctx);
  }

  @Override
  public void enterFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterFunctionDeclaration(ctx);
  }

  @Override
  public void enterRecordDeclaration(EK9Parser.RecordDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterRecordDeclaration(ctx);
  }

  @Override
  public void enterTraitDeclaration(EK9Parser.TraitDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterTraitDeclaration(ctx);
  }

  @Override
  public void enterClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterClassDeclaration(ctx);
  }

  @Override
  public void enterComponentDeclaration(EK9Parser.ComponentDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterComponentDeclaration(ctx);
  }

  @Override
  public void enterTextDeclaration(EK9Parser.TextDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterTextDeclaration(ctx);
  }

  @Override
  public void enterTextBodyDeclaration(EK9Parser.TextBodyDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterTextBodyDeclaration(ctx);
  }

  @Override
  public void enterServiceDeclaration(EK9Parser.ServiceDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterServiceDeclaration(ctx);
  }

  @Override
  public void enterServiceOperationDeclaration(EK9Parser.ServiceOperationDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterServiceOperationDeclaration(ctx);
  }

  @Override
  public void enterApplicationDeclaration(EK9Parser.ApplicationDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterApplicationDeclaration(ctx);
  }

  @Override
  public void enterDynamicClassDeclaration(EK9Parser.DynamicClassDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterDynamicClassDeclaration(ctx);
  }

  @Override
  public void enterDynamicFunctionDeclaration(EK9Parser.DynamicFunctionDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void enterDynamicVariableCapture(EK9Parser.DynamicVariableCaptureContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterDynamicVariableCapture(ctx);
  }

  @Override
  public void enterMethodDeclaration(EK9Parser.MethodDeclarationContext ctx) {
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
  public void enterOperatorDeclaration(EK9Parser.OperatorDeclarationContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterOperatorDeclaration(ctx);
  }

  @Override
  public void enterTypeDeclaration(EK9Parser.TypeDeclarationContext ctx) {
    if (ctx.Identifier() != null) {
      symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    }
    super.enterTypeDeclaration(ctx);
  }

  @Override
  public void enterIfStatement(EK9Parser.IfStatementContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterIfStatement(ctx);
  }

  @Override
  public void enterSwitchStatementExpression(EK9Parser.SwitchStatementExpressionContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterSwitchStatementExpression(ctx);
  }

  @Override
  public void enterForStatement(EK9Parser.ForStatementContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterForStatement(ctx);
  }

  @Override
  public void enterReturningParam(EK9Parser.ReturningParamContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterReturningParam(ctx);
  }

  @Override
  public void enterTryStatementExpression(EK9Parser.TryStatementExpressionContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterTryStatementExpression(ctx);
  }

  @Override
  public void enterCatchStatementExpression(EK9Parser.CatchStatementExpressionContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterCatchStatementExpression(ctx);
  }

  @Override
  public void enterFinallyStatementExpression(EK9Parser.FinallyStatementExpressionContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterFinallyStatementExpression(ctx);
  }

  @Override
  public void enterBlock(EK9Parser.BlockContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterBlock(ctx);
  }

  @Override
  public void enterSingleStatementBlock(EK9Parser.SingleStatementBlockContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterSingleStatementBlock(ctx);
  }

  @Override
  public void enterInstructionBlock(EK9Parser.InstructionBlockContext ctx) {
    symbolAndScopeManagement.enterScope(symbolAndScopeManagement.getRecordedScope(ctx));
    super.enterInstructionBlock(ctx);
  }
}
