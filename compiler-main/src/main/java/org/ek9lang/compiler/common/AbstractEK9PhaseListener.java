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
 * As they are processed it records them in a more permanent manner.
 * A stack is used to help build the aggregates and other constructs. But in the end
 * they are all 'popped off' - our main permanent holding area is the parsedModule!
 * But this code does pull symbols into other scopes, and also checks for exception only
 * paths through code (to a limited static-analysis extent).
 */
public abstract class AbstractEK9PhaseListener extends EK9BaseListener {

  protected final SymbolsAndScopes symbolsAndScopes;
  private final ParsedModule parsedModule;

  protected AbstractEK9PhaseListener(final ParsedModule parsedModule) {

    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);

    this.parsedModule = parsedModule;
    //At construction the ScopeStack will push the module scope on to the stack.
    this.symbolsAndScopes = new SymbolsAndScopes(parsedModule,
        new ScopeStack(parsedModule.getModuleScope()));

  }

  protected void pullSwitchCaseDefaultUp(final EK9Parser.SwitchStatementExpressionContext ctx) {

    final var thisSwitchScope = symbolsAndScopes.getTopScope();

    var noneExceptionPathPossible = false;

    //This is the 'default' if present
    if (ctx.block() != null) {
      noneExceptionPathPossible = symbolsAndScopes.getRecordedScope(ctx.block()).isTerminatedNormally();
    }

    for (var caseStatement : ctx.caseStatement()) {
      noneExceptionPathPossible |=
          symbolsAndScopes.getRecordedScope(caseStatement.block()).isTerminatedNormally();
    }

    //So no none exception paths.
    if (!noneExceptionPathPossible) {
      thisSwitchScope.setEncounteredExceptionToken(new Ek9Token(ctx.start));
    }

  }

  protected void pullTryCatchFinallyUp(final EK9Parser.TryStatementExpressionContext ctx) {

    final var thisTryScope = symbolsAndScopes.getTopScope();

    //assume bad news
    var noneExceptionPathPossible = true;

    //So may get an exception in the block, so it does not terminate normally
    if (ctx.instructionBlock() != null) {
      noneExceptionPathPossible =
          symbolsAndScopes.getRecordedScope(ctx.instructionBlock()).isTerminatedNormally();
    }

    //But if caught and no in the block exception - we're golden - i.e. exception for instruction block consumed.
    if (ctx.catchStatementExpression() != null) {
      noneExceptionPathPossible =
          symbolsAndScopes.getRecordedScope(ctx.catchStatementExpression()).isTerminatedNormally();
    }

    //Now maybe an exception in a finally - block, so this trumps all in an exception.
    if (ctx.finallyStatementExpression() != null) {
      noneExceptionPathPossible &=
          symbolsAndScopes.getRecordedScope(ctx.finallyStatementExpression()).isTerminatedNormally();
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

    final var thisIfScope = symbolsAndScopes.getTopScope();

    //First check the else block for termination, then do all the 'if parts'.
    var abnormal = !symbolsAndScopes.getRecordedScope(ctx.elseOnlyBlock().block()).isTerminatedNormally();

    if (abnormal) {
      boolean normal = ctx.ifControlBlock().stream()
          .map(ifPart -> symbolsAndScopes.getRecordedScope(ifPart.block()))
          .map(IScope::isTerminatedNormally)
          .findAny().orElse(false);
      if (!normal) {
        thisIfScope.setEncounteredExceptionToken(new Ek9Token(ctx.start));
      }
    }

  }

  protected void pullBlockTerminationUp(final ParseTree node) {

    final var scope = symbolsAndScopes.getTopScope();
    final var childScope = symbolsAndScopes.getRecordedScope(node);

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

    return symbolsAndScopes.getTopScope() == null;
  }

  @Override
  public void exitModuleDeclaration(final EK9Parser.ModuleDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitModuleDeclaration(ctx);

  }

  @Override
  public void exitPackageBlock(final EK9Parser.PackageBlockContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitPackageBlock(ctx);

  }

  @Override
  public void exitMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    //This will pop the synthetic main method off for the program
    //Program is sort of a method, but the structure is slightly different.
    symbolsAndScopes.exitScope();
    //Now the parent scope can be popped off, but only if it was put on.
    if (ctx.getParent() instanceof EK9Parser.ProgramBlockContext && ctx.operationDetails() != null) {
      symbolsAndScopes.exitScope();
    }

    super.exitMethodDeclaration(ctx);

  }

  @Override
  public void exitOperatorDeclaration(final EK9Parser.OperatorDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitOperatorDeclaration(ctx);

  }

  @Override
  public void exitFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitFunctionDeclaration(ctx);

  }

  @Override
  public void exitRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitRecordDeclaration(ctx);

  }

  @Override
  public void exitTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitTraitDeclaration(ctx);

  }

  @Override
  public void exitClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitClassDeclaration(ctx);

  }

  @Override
  public void exitComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitComponentDeclaration(ctx);

  }

  @Override
  public void exitTextDeclaration(final EK9Parser.TextDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitTextDeclaration(ctx);

  }

  @Override
  public void exitTextBodyDeclaration(final EK9Parser.TextBodyDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitTextBodyDeclaration(ctx);

  }

  @Override
  public void exitServiceDeclaration(final EK9Parser.ServiceDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitServiceDeclaration(ctx);

  }

  @Override
  public void exitServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitServiceOperationDeclaration(ctx);

  }

  @Override
  public void exitApplicationDeclaration(final EK9Parser.ApplicationDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitApplicationDeclaration(ctx);

  }

  @Override
  public void exitDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitDynamicClassDeclaration(ctx);

  }

  @Override
  public void exitDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitDynamicFunctionDeclaration(ctx);

  }

  @Override
  public void exitDynamicVariableCapture(final EK9Parser.DynamicVariableCaptureContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitDynamicVariableCapture(ctx);

  }

  @Override
  public void exitTypeDeclaration(final EK9Parser.TypeDeclarationContext ctx) {

    if (ctx.Identifier() != null) {
      symbolsAndScopes.exitScope();
    }
    super.exitTypeDeclaration(ctx);

  }

  @Override
  public void exitIfStatement(final EK9Parser.IfStatementContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitIfStatement(ctx);

  }

  @Override
  public void exitSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitSwitchStatementExpression(ctx);

  }

  @Override
  public void exitForStatementExpression(final EK9Parser.ForStatementExpressionContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitForStatementExpression(ctx);

  }

  @Override
  public void exitWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitWhileStatementExpression(ctx);

  }

  @Override
  public void exitReturningParam(final EK9Parser.ReturningParamContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitReturningParam(ctx);

  }

  @Override
  public void exitTryStatementExpression(final EK9Parser.TryStatementExpressionContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitTryStatementExpression(ctx);

  }

  @Override
  public void exitCatchStatementExpression(final EK9Parser.CatchStatementExpressionContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitCatchStatementExpression(ctx);

  }

  @Override
  public void exitFinallyStatementExpression(final EK9Parser.FinallyStatementExpressionContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitFinallyStatementExpression(ctx);

  }

  @Override
  public void exitTernaryPart(EK9Parser.TernaryPartContext ctx) {
    symbolsAndScopes.exitScope();
    super.exitTernaryPart(ctx);
  }

  @Override
  public void exitBlock(final EK9Parser.BlockContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitBlock(ctx);

  }

  @Override
  public void exitSingleStatementBlock(final EK9Parser.SingleStatementBlockContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitSingleStatementBlock(ctx);

  }

  @Override
  public void exitInstructionBlock(final EK9Parser.InstructionBlockContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitInstructionBlock(ctx);

  }

}
