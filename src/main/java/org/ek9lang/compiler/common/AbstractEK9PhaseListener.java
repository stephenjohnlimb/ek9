package org.ek9lang.compiler.common;

import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.antlr.EK9BaseListener;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;

/**
 * The abstract base on most antlr listeners. This class does little except ensure that
 * the way ANTLR is designed we have to listen to events and push
 * our constructed symbols into a stack and also pop them off again.
 * As they are processed we also have to record them in a more permanent manner.
 * So the stack is used to help build the aggregates etc. But in the end
 * they are all 'popped off' - our main permanent holding area is the parsedModule!
 * But this code does pull symbols into other scopes, and also checks for exception only
 * paths through code (to a limited static-analysis extent).
 */
public abstract class AbstractEK9PhaseListener extends EK9BaseListener {

  protected final SymbolAndScopeManagement symbolAndScopeManagement;
  private final ParsedModule parsedModule;

  protected AbstractEK9PhaseListener(ParsedModule parsedModule) {
    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);

    this.parsedModule = parsedModule;

    //At construction the ScopeStack will push the module scope on to the stack.
    this.symbolAndScopeManagement = new SymbolAndScopeManagement(parsedModule,
        new ScopeStack(parsedModule.getModuleScope()));
  }

  protected void pullSwitchCaseDefaultUp(EK9Parser.SwitchStatementExpressionContext ctx) {
    var noneExceptionPathPossible = false;
    var thisSwitchScope = symbolAndScopeManagement.getTopScope();

    //This is the 'default' if present
    if (ctx.block() != null) {
      noneExceptionPathPossible = symbolAndScopeManagement.getRecordedScope(ctx.block()).isTerminatedNormally();
    }

    for (var caseStatement : ctx.caseStatement()) {
      noneExceptionPathPossible |=
          symbolAndScopeManagement.getRecordedScope(caseStatement.block()).isTerminatedNormally();
    }

    //So no none exception paths.
    if (!noneExceptionPathPossible) {
      thisSwitchScope.setEncounteredExceptionToken(new Ek9Token(ctx.start));
    }
  }

  protected void pullTryCatchFinallyUp(final EK9Parser.TryStatementExpressionContext ctx) {
    //assume bad news
    var noneExceptionPathPossible = true;
    IScope thisTryScope = symbolAndScopeManagement.getTopScope();

    //So may get an exception in the block, so it does not terminate normally
    if (ctx.instructionBlock() != null) {
      noneExceptionPathPossible =
          symbolAndScopeManagement.getRecordedScope(ctx.instructionBlock()).isTerminatedNormally();
    }

    //But if caught and no in the block exception - we're golden - i.e. exception for instruction block consumed.
    if (ctx.catchStatementExpression() != null) {
      noneExceptionPathPossible =
          symbolAndScopeManagement.getRecordedScope(ctx.catchStatementExpression()).isTerminatedNormally();
    }

    //Now maybe an exception in a finally - block, so this trumps all in an exception.
    if (ctx.finallyStatementExpression() != null) {
      noneExceptionPathPossible &=
          symbolAndScopeManagement.getRecordedScope(ctx.finallyStatementExpression()).isTerminatedNormally();
    }

    //So no none exception paths.
    if (!noneExceptionPathPossible) {
      thisTryScope.setEncounteredExceptionToken(new Ek9Token(ctx.start));
    }
  }

  protected void pullIfElseTerminationUp(final EK9Parser.IfStatementContext ctx) {

    if (ctx.elseOnlyBlock() == null) {
      //If it was just an if then - it may or may not terminate normally
      return;
    }
    IScope thisIfScope = symbolAndScopeManagement.getTopScope();

    //First check the else block for termination, then do all the 'if parts'.
    var abnormal = !symbolAndScopeManagement.getRecordedScope(ctx.elseOnlyBlock().block()).isTerminatedNormally();

    if (abnormal) {
      boolean normal = ctx.ifControlBlock().stream()
          .map(ifPart -> symbolAndScopeManagement.getRecordedScope(ifPart.block()))
          .map(IScope::isTerminatedNormally)
          .findAny().orElse(false);
      if (!normal) {
        thisIfScope.setEncounteredExceptionToken(new Ek9Token(ctx.start));
      }
    }
  }

  protected void pullBlockTerminationUp(final ParseTree node) {
    IScope scope = symbolAndScopeManagement.getTopScope();
    var childScope = symbolAndScopeManagement.getRecordedScope(node);

    if (childScope != null && !childScope.isTerminatedNormally()) {
      scope.setEncounteredExceptionToken(childScope.getEncounteredExceptionToken());
    }
  }


  /**
   * Provide access to the parsedModule.
   */
  protected ParsedModule getParsedModule() {
    return parsedModule;
  }

  /**
   * Provide access to errorListener to extending Listeners.
   * This enables reporting of errors and warnings.
   */
  public ErrorListener getErrorListener() {
    return parsedModule.getSource().getErrorListener();
  }

  public boolean isScopeStackEmpty() {
    return symbolAndScopeManagement.getTopScope() == null;
  }

  @Override
  public void exitModuleDeclaration(EK9Parser.ModuleDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitModuleDeclaration(ctx);
  }

  @Override
  public void exitPackageBlock(EK9Parser.PackageBlockContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitPackageBlock(ctx);
  }

  @Override
  public void exitMethodDeclaration(EK9Parser.MethodDeclarationContext ctx) {
    //This will pop the synthetic main method off for the program
    symbolAndScopeManagement.exitScope();
    //Now the parent scope can be popped off.
    if (ctx.getParent() instanceof EK9Parser.ProgramBlockContext) {
      symbolAndScopeManagement.exitScope();
    }
    super.exitMethodDeclaration(ctx);
  }

  @Override
  public void exitOperatorDeclaration(EK9Parser.OperatorDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitOperatorDeclaration(ctx);
  }

  @Override
  public void exitFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitFunctionDeclaration(ctx);
  }

  @Override
  public void exitRecordDeclaration(EK9Parser.RecordDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitRecordDeclaration(ctx);
  }

  @Override
  public void exitTraitDeclaration(EK9Parser.TraitDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitTraitDeclaration(ctx);
  }

  @Override
  public void exitClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitClassDeclaration(ctx);
  }

  @Override
  public void exitComponentDeclaration(EK9Parser.ComponentDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitComponentDeclaration(ctx);
  }

  @Override
  public void exitTextDeclaration(EK9Parser.TextDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitTextDeclaration(ctx);
  }

  @Override
  public void exitTextBodyDeclaration(EK9Parser.TextBodyDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitTextBodyDeclaration(ctx);
  }

  @Override
  public void exitServiceDeclaration(EK9Parser.ServiceDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitServiceDeclaration(ctx);
  }

  @Override
  public void exitServiceOperationDeclaration(EK9Parser.ServiceOperationDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitServiceOperationDeclaration(ctx);
  }

  @Override
  public void exitApplicationDeclaration(EK9Parser.ApplicationDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitApplicationDeclaration(ctx);
  }

  @Override
  public void exitDynamicClassDeclaration(EK9Parser.DynamicClassDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitDynamicClassDeclaration(ctx);
  }

  @Override
  public void exitDynamicFunctionDeclaration(EK9Parser.DynamicFunctionDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void exitDynamicVariableCapture(EK9Parser.DynamicVariableCaptureContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitDynamicVariableCapture(ctx);
  }

  @Override
  public void exitTypeDeclaration(EK9Parser.TypeDeclarationContext ctx) {
    if (ctx.Identifier() != null) {
      symbolAndScopeManagement.exitScope();
    }
    super.exitTypeDeclaration(ctx);
  }

  @Override
  public void exitIfStatement(EK9Parser.IfStatementContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitIfStatement(ctx);
  }

  @Override
  public void exitSwitchStatementExpression(EK9Parser.SwitchStatementExpressionContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitSwitchStatementExpression(ctx);
  }

  @Override
  public void exitForStatementExpression(EK9Parser.ForStatementExpressionContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitForStatementExpression(ctx);
  }

  @Override
  public void exitWhileStatementExpression(EK9Parser.WhileStatementExpressionContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitWhileStatementExpression(ctx);
  }

  @Override
  public void exitReturningParam(EK9Parser.ReturningParamContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitReturningParam(ctx);
  }

  @Override
  public void exitTryStatementExpression(EK9Parser.TryStatementExpressionContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitTryStatementExpression(ctx);
  }

  @Override
  public void exitCatchStatementExpression(EK9Parser.CatchStatementExpressionContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitCatchStatementExpression(ctx);
  }

  @Override
  public void exitFinallyStatementExpression(EK9Parser.FinallyStatementExpressionContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitFinallyStatementExpression(ctx);
  }

  @Override
  public void exitBlock(EK9Parser.BlockContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitBlock(ctx);
  }

  @Override
  public void exitSingleStatementBlock(EK9Parser.SingleStatementBlockContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitSingleStatementBlock(ctx);
  }

  @Override
  public void exitInstructionBlock(EK9Parser.InstructionBlockContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitInstructionBlock(ctx);
  }
}
