package org.ek9lang.compiler.phase3;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ScopeStackConsistencyListener;
import org.ek9lang.compiler.support.SymbolFactory;

/**
 * This listener just deals with expressions and the types that result from expressions.
 * Note that the correct must be in place for these expressions to 'reside in'.
 * See 'instructionBlock' and enter/exit in the 'ScopeStackConsistencyListener' and 'AbstractEK9PhaseListener'.
 * Now some of this may feel upside down, processing on 'exit' and from the 'bottom up'.
 */
abstract class ExpressionsListener extends ScopeStackConsistencyListener {

  protected final SymbolFactory symbolFactory;
  protected final ErrorListener errorListener;
  private final ThisOrSuperOrError thisOrSuperOrError;
  private final PrimaryOrError primaryOrError;
  private final IdentifierReferenceOrError identifierReferenceOrError;
  private final StatementOrError statementOrError;
  private final ExpressionOrError expressionOrError;
  private final InstructionBlockVariablesOrError instructionBlockVariablesOrError;
  private final AssignmentExpressionOrError assignmentExpressionOrError;
  private final AssignmentStatementOrError assignmentStatementOrError;
  private final ListUseOrError listUseOrError;
  private final DictUseOrError dictUseOrError;
  private final CallOrError callOrError;
  private final RangeOrError rangeOrError;
  private final ForLoopOrError forLoopOrError;
  private final ForRangeOrError forRangeOrError;
  private final VariableAssignmentOrError variableAssignmentOrError;
  private final VariableOnlyOrError variableOnlyOrError;
  private final StreamCatOrError streamCatOrError;
  private final StreamForOrError streamForOrError;
  private final PipelinePartOrError pipelinePartOrError;
  private final StreamStatementTerminationOrError streamStatementTerminationOrError;
  private final StreamStatementOrError streamStatementOrError;
  private final StreamExpressionTerminationOrError streamExpressionTerminationOrError;
  private final StreamExpressionOrError streamExpressionOrError;
  private final ObjectAccessStartOrError objectAccessStartOrError;
  private final ObjectAccessExpressionOrError objectAccessExpressionOrError;
  private final PreFlowStatementOrError preFlowStatementOrError;
  private final GuardExpressionOrError guardExpressionOrError;
  private final IfStatementOrError ifStatementOrError;
  private final ForStatementExpressionOrError forStatementExpressionOrError;
  private final WhileStatementExpressionOrError whileStatementExpressionOrError;
  private final ProcessWhileControlVariable processWhileControlVariable;
  private final SwitchStatementExpressionOrError switchStatementExpressionOrError;
  private final SwitchReturnOrError switchReturnOrError;
  private final TryReturnOrError tryReturnOrError;
  private final WhileReturnOrError whileReturnOrError;
  private final ForReturnOrError forReturnOrError;
  private final TryStatementExpressionOrError tryStatementExpressionOrError;
  private final ThrowStatementOrError throwStatementOrError;
  private final AssertStatementOrError assertStatementOrError;

  protected ExpressionsListener(ParsedModule parsedModule) {

    super(parsedModule);

    this.symbolFactory =
        new SymbolFactory(parsedModule);
    this.errorListener =
        parsedModule.getSource().getErrorListener();
    this.thisOrSuperOrError =
        new ThisOrSuperOrError(symbolsAndScopes, symbolFactory, errorListener);
    this.primaryOrError =
        new PrimaryOrError(symbolsAndScopes, errorListener);
    this.identifierReferenceOrError =
        new IdentifierReferenceOrError(symbolsAndScopes, errorListener);
    this.statementOrError =
        new StatementOrError(symbolsAndScopes, errorListener);
    this.expressionOrError =
        new ExpressionOrError(symbolsAndScopes, symbolFactory, errorListener);
    this.instructionBlockVariablesOrError =
        new InstructionBlockVariablesOrError(symbolsAndScopes, errorListener);
    this.assignmentExpressionOrError =
        new AssignmentExpressionOrError(symbolsAndScopes, errorListener);
    this.assignmentStatementOrError =
        new AssignmentStatementOrError(symbolsAndScopes, errorListener);
    this.callOrError =
        new CallOrError(symbolsAndScopes, symbolFactory, errorListener);
    this.listUseOrError =
        new ListUseOrError(symbolsAndScopes, symbolFactory, errorListener);
    this.dictUseOrError =
        new DictUseOrError(symbolsAndScopes, symbolFactory, errorListener);
    this.streamCatOrError =
        new StreamCatOrError(symbolsAndScopes, errorListener);
    this.streamForOrError =
        new StreamForOrError(symbolsAndScopes, errorListener);
    this.pipelinePartOrError =
        new PipelinePartOrError(symbolsAndScopes, errorListener);
    this.streamStatementTerminationOrError =
        new StreamStatementTerminationOrError(symbolsAndScopes, errorListener);
    this.streamStatementOrError =
        new StreamStatementOrError(symbolsAndScopes, symbolFactory, errorListener);
    this.streamExpressionTerminationOrError =
        new StreamExpressionTerminationOrError(symbolsAndScopes, errorListener);
    this.streamExpressionOrError =
        new StreamExpressionOrError(symbolsAndScopes, symbolFactory, errorListener);
    this.rangeOrError =
        new RangeOrError(symbolsAndScopes, symbolFactory, errorListener);
    this.forLoopOrError =
        new ForLoopOrError(symbolsAndScopes, errorListener);
    this.forRangeOrError =
        new ForRangeOrError(symbolsAndScopes, errorListener);
    this.variableAssignmentOrError =
        new VariableAssignmentOrError(symbolsAndScopes, errorListener);
    this.variableOnlyOrError =
        new VariableOnlyOrError(symbolsAndScopes, errorListener);
    this.objectAccessStartOrError =
        new ObjectAccessStartOrError(symbolsAndScopes, errorListener);
    this.objectAccessExpressionOrError =
        new ObjectAccessExpressionOrError(symbolsAndScopes, errorListener);
    this.guardExpressionOrError =
        new GuardExpressionOrError(symbolsAndScopes, errorListener);
    this.preFlowStatementOrError =
        new PreFlowStatementOrError(symbolsAndScopes, errorListener);
    this.ifStatementOrError =
        new IfStatementOrError(symbolsAndScopes, errorListener);
    this.forStatementExpressionOrError =
        new ForStatementExpressionOrError(symbolsAndScopes, errorListener);
    this.whileStatementExpressionOrError =
        new WhileStatementExpressionOrError(symbolsAndScopes, errorListener);
    this.processWhileControlVariable =
        new ProcessWhileControlVariable(symbolsAndScopes, errorListener);
    this.switchStatementExpressionOrError =
        new SwitchStatementExpressionOrError(symbolsAndScopes, errorListener);
    this.switchReturnOrError =
        new SwitchReturnOrError(errorListener);
    this.tryStatementExpressionOrError =
        new TryStatementExpressionOrError(symbolsAndScopes, errorListener);
    this.throwStatementOrError =
        new ThrowStatementOrError(symbolsAndScopes, errorListener);
    this.tryReturnOrError =
        new TryReturnOrError(errorListener);
    this.whileReturnOrError =
        new WhileReturnOrError(errorListener);
    this.forReturnOrError =
        new ForReturnOrError(errorListener);
    this.assertStatementOrError =
        new AssertStatementOrError(symbolsAndScopes, errorListener);
  }

  @Override
  public void exitIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {
    identifierReferenceOrError.apply(ctx);
    super.exitIdentifierReference(ctx);
  }

  @Override
  public void exitTraitReference(final EK9Parser.TraitReferenceContext ctx) {
    final var traitReference = symbolsAndScopes.getRecordedSymbol(ctx.identifierReference());
    if (traitReference != null) {
      symbolsAndScopes.recordSymbol(traitReference, ctx);
    }
    super.exitTraitReference(ctx);
  }

  @Override
  public void exitPrimaryReference(final EK9Parser.PrimaryReferenceContext ctx) {
    //Here we are modelling 'this' or 'super', idea is to set type or issue errors.
    thisOrSuperOrError.accept(ctx);
    super.exitPrimaryReference(ctx);
  }

  @Override
  public void exitPrimary(final EK9Parser.PrimaryContext ctx) {
    primaryOrError.accept(ctx);
    super.exitPrimary(ctx);
  }

  @Override
  public void exitCall(final EK9Parser.CallContext ctx) {
    callOrError.accept(ctx);
    super.exitCall(ctx);
  }

  @Override
  public void exitList(final EK9Parser.ListContext ctx) {
    listUseOrError.accept(ctx);
    super.exitList(ctx);
  }

  @Override
  public void exitDict(final EK9Parser.DictContext ctx) {
    dictUseOrError.accept(ctx);
    super.exitDict(ctx);
  }

  @Override
  public void exitRange(final EK9Parser.RangeContext ctx) {
    rangeOrError.accept(ctx);
    super.exitRange(ctx);
  }

  @Override
  public void exitForLoop(final EK9Parser.ForLoopContext ctx) {
    forLoopOrError.accept(ctx);
    super.exitForLoop(ctx);
  }

  @Override
  public void exitForRange(final EK9Parser.ForRangeContext ctx) {
    forRangeOrError.accept(ctx);
    super.exitForRange(ctx);
  }

  @Override
  public void exitStatement(final EK9Parser.StatementContext ctx) {
    statementOrError.accept(ctx);
    super.exitStatement(ctx);
  }

  @Override
  public void exitExpression(final EK9Parser.ExpressionContext ctx) {
    expressionOrError.accept(ctx);
    super.exitExpression(ctx);
  }

  @Override
  public void exitAssignmentExpression(final EK9Parser.AssignmentExpressionContext ctx) {
    assignmentExpressionOrError.accept(ctx);
    super.exitAssignmentExpression(ctx);
  }

  @Override
  public void exitAssignmentStatement(final EK9Parser.AssignmentStatementContext ctx) {
    assignmentStatementOrError.accept(ctx);
    super.exitAssignmentStatement(ctx);
  }

  @Override
  public void exitIfStatement(final EK9Parser.IfStatementContext ctx) {
    ifStatementOrError.accept(ctx);
    super.exitIfStatement(ctx);
  }

  @Override
  public void exitPreFlowStatement(EK9Parser.PreFlowStatementContext ctx) {
    preFlowStatementOrError.accept(ctx);
    super.exitPreFlowStatement(ctx);
  }

  @Override
  public void exitGuardExpression(final EK9Parser.GuardExpressionContext ctx) {
    guardExpressionOrError.accept(ctx);
    super.exitGuardExpression(ctx);
  }

  @Override
  public void exitForStatementExpression(final EK9Parser.ForStatementExpressionContext ctx) {
    forStatementExpressionOrError.andThen(forReturnOrError).accept(ctx);
    super.exitForStatementExpression(ctx);
  }

  @Override
  public void enterWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {
    //Now here it is important to check the while 'control' variable.
    processWhileControlVariable.andThen(whileReturnOrError).accept(ctx);

    super.enterWhileStatementExpression(ctx);
  }

  @Override
  public void exitWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {
    whileStatementExpressionOrError.accept(ctx);
    super.exitWhileStatementExpression(ctx);
  }

  @Override
  public void enterSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {
    //Need to do the basics checks and issue errors on switches before assessing types later in the exit.
    switchReturnOrError.accept(ctx);
    super.enterSwitchStatementExpression(ctx);
  }

  @Override
  public void exitSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {
    //Now a more detailed analysis can take place.
    switchStatementExpressionOrError.accept(ctx);
    super.exitSwitchStatementExpression(ctx);
  }

  @Override
  public void enterTryStatementExpression(final EK9Parser.TryStatementExpressionContext ctx) {
    tryReturnOrError.accept(ctx);
    super.enterTryStatementExpression(ctx);
  }

  @Override
  public void exitAssertStatement(final EK9Parser.AssertStatementContext ctx) {
    assertStatementOrError.accept(ctx);
    super.exitAssertStatement(ctx);
  }

  @Override
  public void exitThrowStatement(final EK9Parser.ThrowStatementContext ctx) {
    throwStatementOrError.accept(ctx);
    super.exitThrowStatement(ctx);
  }

  @Override
  public void exitTryStatementExpression(final EK9Parser.TryStatementExpressionContext ctx) {
    tryStatementExpressionOrError.accept(ctx);
    super.exitTryStatementExpression(ctx);
  }

  @Override
  public void exitInstructionBlock(final EK9Parser.InstructionBlockContext ctx) {
    instructionBlockVariablesOrError.accept(ctx);
    super.exitInstructionBlock(ctx);
  }

  @Override
  public void exitObjectAccessStart(final EK9Parser.ObjectAccessStartContext ctx) {
    objectAccessStartOrError.accept(ctx);
    super.exitObjectAccessStart(ctx);
  }

  @Override
  public void exitObjectAccessExpression(final EK9Parser.ObjectAccessExpressionContext ctx) {
    objectAccessExpressionOrError.accept(ctx);
    super.exitObjectAccessExpression(ctx);
  }

  @Override
  public void exitVariableDeclaration(final EK9Parser.VariableDeclarationContext ctx) {
    variableAssignmentOrError.accept(ctx);
    super.exitVariableDeclaration(ctx);
  }

  @Override
  public void exitVariableOnlyDeclaration(final EK9Parser.VariableOnlyDeclarationContext ctx) {
    variableOnlyOrError.accept(ctx);
    super.exitVariableOnlyDeclaration(ctx);
  }

  @Override
  public void exitStreamCat(final EK9Parser.StreamCatContext ctx) {
    streamCatOrError.accept(ctx);
    super.exitStreamCat(ctx);
  }

  @Override
  public void exitStreamFor(final EK9Parser.StreamForContext ctx) {
    streamForOrError.accept(ctx);
    super.exitStreamFor(ctx);
  }

  @Override
  public void exitPipelinePart(final EK9Parser.PipelinePartContext ctx) {
    pipelinePartOrError.accept(ctx);
    super.exitPipelinePart(ctx);
  }

  @Override
  public void exitStreamStatementTermination(final EK9Parser.StreamStatementTerminationContext ctx) {
    streamStatementTerminationOrError.accept(ctx);
    super.exitStreamStatementTermination(ctx);
  }

  @Override
  public void exitStreamStatement(final EK9Parser.StreamStatementContext ctx) {
    streamStatementOrError.accept(ctx);
    super.exitStreamStatement(ctx);
  }

  @Override
  public void exitStreamExpressionTermination(final EK9Parser.StreamExpressionTerminationContext ctx) {
    streamExpressionTerminationOrError.accept(ctx);
    super.exitStreamExpressionTermination(ctx);
  }

  @Override
  public void exitStreamExpression(final EK9Parser.StreamExpressionContext ctx) {
    streamExpressionOrError.accept(ctx);
    super.exitStreamExpression(ctx);
  }

}
