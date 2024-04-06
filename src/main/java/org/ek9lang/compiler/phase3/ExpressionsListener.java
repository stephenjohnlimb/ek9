package org.ek9lang.compiler.phase3;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ScopeStackConsistencyListener;
import org.ek9lang.compiler.support.SymbolFactory;

/**
 * This listener just deals with expressions and the types that result from expressions.
 * Note that the correct must be inplace for these expressions to 'reside in'.
 * See 'instructionBlock' and enter/exit in the 'ScopeStackConsistencyListener' and 'AbstractEK9PhaseListener'.
 * Now some of this may feel upside down, processing on 'exit' and from the 'bottom up'.
 */
abstract class ExpressionsListener extends ScopeStackConsistencyListener {
  
  protected final SymbolFactory symbolFactory;
  protected final ErrorListener errorListener;
  private final ProcessValidThisOrSuper processValidThisOrSuper;
  private final ProcessValidPrimary processValidPrimary;
  private final ProcessValidIdentifierReference processValidIdentifierReference;
  private final CheckValidStatement checkValidStatement;
  private final CheckValidExpression checkValidExpression;
  private final CheckInstructionBlockVariables checkInstructionBlockVariables;
  private final ProcessAssignmentExpression processAssignmentExpression;
  private final ProcessAssignmentStatement processAssignmentStatement;
  private final ProcessAndTypeList processAndTypeList;
  private final ProcessAndTypeDict processAndTypeDict;
  private final CheckValidCall checkValidCall;
  private final ProcessRange processRange;
  private final ProcessForLoop processForLoop;
  private final ProcessForRange processForRange;
  private final CheckVariableAssignment checkVariableAssignment;
  private final CheckVariableOnlyDeclaration checkVariableOnlyDeclaration;
  private final ProcessStreamCat processStreamCat;
  private final ProcessStreamFor processStreamFor;
  private final ProcessPipelinePart processPipelinePart;
  private final ProcessStreamStatementTermination processStreamStatementTermination;
  private final ProcessStreamStatement processStreamStatement;
  private final ProcessStreamExpressionTermination processStreamExpressionTermination;
  private final ProcessStreamExpression processStreamExpression;
  private final ProcessObjectAccessStartOrError processObjectAccessStartOrError;
  private final ProcessObjectAccessExpressionOrError processObjectAccessExpressionOrError;
  private final ProcessGuardExpression processGuardExpression;
  private final ProcessIfStatement processIfStatement;
  private final ProcessForStatementExpression processForStatementExpression;
  private final ProcessWhileStatementExpression processWhileStatementExpression;
  private final ProcessWhileControlVariable processWhileControlVariable;
  private final ProcessSwitchStatementExpression processSwitchStatementExpression;
  private final CheckSwitch checkSwitch;
  private final ProcessTryStatementExpression processTryStatementExpression;
  private final ProcessCaseExpression processCaseExpression;
  private final CheckThrowStatementOrError checkThrowStatementOrError;

  protected ExpressionsListener(ParsedModule parsedModule) {
    
    super(parsedModule);

    this.symbolFactory =
        new SymbolFactory(parsedModule);
    this.errorListener =
        parsedModule.getSource().getErrorListener();
    this.processValidThisOrSuper =
        new ProcessValidThisOrSuper(symbolAndScopeManagement, symbolFactory, errorListener);
    this.processValidPrimary =
        new ProcessValidPrimary(symbolAndScopeManagement, errorListener);
    this.processValidIdentifierReference =
        new ProcessValidIdentifierReference(symbolAndScopeManagement, errorListener);
    this.checkValidStatement =
        new CheckValidStatement(symbolAndScopeManagement, errorListener);
    this.checkValidExpression =
        new CheckValidExpression(symbolAndScopeManagement, symbolFactory, errorListener);
    this.checkInstructionBlockVariables =
        new CheckInstructionBlockVariables(symbolAndScopeManagement, errorListener);
    this.processAssignmentExpression =
        new ProcessAssignmentExpression(symbolAndScopeManagement, errorListener);
    this.processAssignmentStatement =
        new ProcessAssignmentStatement(symbolAndScopeManagement, errorListener);
    this.checkValidCall =
        new CheckValidCall(symbolAndScopeManagement, symbolFactory, errorListener);
    this.processAndTypeList =
        new ProcessAndTypeList(symbolAndScopeManagement, symbolFactory, errorListener);
    this.processAndTypeDict =
        new ProcessAndTypeDict(symbolAndScopeManagement, symbolFactory, errorListener);
    this.processStreamCat =
        new ProcessStreamCat(symbolAndScopeManagement, errorListener);
    this.processStreamFor =
        new ProcessStreamFor(symbolAndScopeManagement, errorListener);
    this.processPipelinePart =
        new ProcessPipelinePart(symbolAndScopeManagement, errorListener);
    this.processStreamStatementTermination =
        new ProcessStreamStatementTermination(symbolAndScopeManagement, errorListener);
    this.processStreamStatement =
        new ProcessStreamStatement(symbolAndScopeManagement, symbolFactory, errorListener);
    this.processStreamExpressionTermination =
        new ProcessStreamExpressionTermination(symbolAndScopeManagement, errorListener);
    this.processStreamExpression =
        new ProcessStreamExpression(symbolAndScopeManagement, symbolFactory, errorListener);
    this.processRange =
        new ProcessRange(symbolAndScopeManagement, symbolFactory, errorListener);
    this.processForLoop =
        new ProcessForLoop(symbolAndScopeManagement, errorListener);
    this.processForRange =
        new ProcessForRange(symbolAndScopeManagement, errorListener);
    this.checkVariableAssignment =
        new CheckVariableAssignment(symbolAndScopeManagement, errorListener);
    this.checkVariableOnlyDeclaration =
        new CheckVariableOnlyDeclaration(symbolAndScopeManagement, errorListener);
    this.processObjectAccessStartOrError =
        new ProcessObjectAccessStartOrError(symbolAndScopeManagement, errorListener);
    this.processObjectAccessExpressionOrError =
        new ProcessObjectAccessExpressionOrError(symbolAndScopeManagement, errorListener);
    this.processGuardExpression =
        new ProcessGuardExpression(symbolAndScopeManagement, errorListener);
    this.processIfStatement =
        new ProcessIfStatement(symbolAndScopeManagement, errorListener);
    this.processForStatementExpression =
        new ProcessForStatementExpression(symbolAndScopeManagement, errorListener);
    this.processWhileStatementExpression =
        new ProcessWhileStatementExpression(symbolAndScopeManagement, errorListener);
    this.processWhileControlVariable =
        new ProcessWhileControlVariable(symbolAndScopeManagement, errorListener);
    this.processSwitchStatementExpression =
        new ProcessSwitchStatementExpression(symbolAndScopeManagement, errorListener);
    this.checkSwitch =
        new CheckSwitch(errorListener);
    this.processCaseExpression =
        new ProcessCaseExpression(symbolAndScopeManagement, errorListener);
    this.processTryStatementExpression =
        new ProcessTryStatementExpression(symbolAndScopeManagement, errorListener);
    this.checkThrowStatementOrError =
        new CheckThrowStatementOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void exitIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {
    processValidIdentifierReference.apply(ctx);
    super.exitIdentifierReference(ctx);
  }

  @Override
  public void exitTraitReference(final EK9Parser.TraitReferenceContext ctx) {
    final var traitReference = symbolAndScopeManagement.getRecordedSymbol(ctx.identifierReference());
    if (traitReference != null) {
      symbolAndScopeManagement.recordSymbol(traitReference, ctx);
    }
    super.exitTraitReference(ctx);
  }

  @Override
  public void exitPrimaryReference(final EK9Parser.PrimaryReferenceContext ctx) {
    //Here we are modelling 'this' or 'super', idea is to set type or issue errors.
    processValidThisOrSuper.accept(ctx);
    super.exitPrimaryReference(ctx);
  }

  @Override
  public void exitPrimary(final EK9Parser.PrimaryContext ctx) {
    processValidPrimary.accept(ctx);
    super.exitPrimary(ctx);
  }

  @Override
  public void exitCall(final EK9Parser.CallContext ctx) {
    checkValidCall.accept(ctx);
    super.exitCall(ctx);
  }

  @Override
  public void exitList(final EK9Parser.ListContext ctx) {
    processAndTypeList.accept(ctx);
    super.exitList(ctx);
  }

  @Override
  public void exitDict(final EK9Parser.DictContext ctx) {
    processAndTypeDict.accept(ctx);
    super.exitDict(ctx);
  }

  @Override
  public void exitRange(final EK9Parser.RangeContext ctx) {
    processRange.accept(ctx);
    super.exitRange(ctx);
  }

  @Override
  public void exitForLoop(final EK9Parser.ForLoopContext ctx) {
    processForLoop.accept(ctx);
    super.exitForLoop(ctx);
  }

  @Override
  public void exitForRange(final EK9Parser.ForRangeContext ctx) {
    processForRange.accept(ctx);
    super.exitForRange(ctx);
  }

  @Override
  public void exitStatement(final EK9Parser.StatementContext ctx) {
    checkValidStatement.accept(ctx);
    super.exitStatement(ctx);
  }

  @Override
  public void exitExpression(final EK9Parser.ExpressionContext ctx) {
    checkValidExpression.accept(ctx);
    super.exitExpression(ctx);
  }

  @Override
  public void exitAssignmentExpression(final EK9Parser.AssignmentExpressionContext ctx) {
    processAssignmentExpression.accept(ctx);
    super.exitAssignmentExpression(ctx);
  }

  @Override
  public void exitAssignmentStatement(final EK9Parser.AssignmentStatementContext ctx) {
    processAssignmentStatement.accept(ctx);
    super.exitAssignmentStatement(ctx);
  }

  @Override
  public void exitIfStatement(final EK9Parser.IfStatementContext ctx) {
    processIfStatement.accept(ctx);
    super.exitIfStatement(ctx);
  }

  @Override
  public void exitGuardExpression(final EK9Parser.GuardExpressionContext ctx) {
    processGuardExpression.accept(ctx);
    super.exitGuardExpression(ctx);
  }

  @Override
  public void exitForStatementExpression(final EK9Parser.ForStatementExpressionContext ctx) {
    processForStatementExpression.accept(ctx);
    super.exitForStatementExpression(ctx);
  }

  @Override
  public void enterWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {
    //Now here it is important to check the while 'control' variable.
    processWhileControlVariable.accept(ctx);
    super.enterWhileStatementExpression(ctx);
  }

  @Override
  public void exitWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {
    processWhileStatementExpression.accept(ctx);
    super.exitWhileStatementExpression(ctx);
  }

  @Override
  public void enterSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {
    //Need to do the basics checks and issue errors on switches before assessing types later in the exit.
    checkSwitch.accept(ctx);
    super.enterSwitchStatementExpression(ctx);
  }

  @Override
  public void exitSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {
    //Now a more detailed analysis can take place.
    processSwitchStatementExpression.accept(ctx);
    super.exitSwitchStatementExpression(ctx);
  }

  @Override
  public void exitThrowStatement(final EK9Parser.ThrowStatementContext ctx) {
    checkThrowStatementOrError.accept(ctx);
    super.exitThrowStatement(ctx);
  }

  @Override
  public void exitCaseExpression(final EK9Parser.CaseExpressionContext ctx) {
    processCaseExpression.accept(ctx);
    super.exitCaseExpression(ctx);
  }

  @Override
  public void exitTryStatementExpression(final EK9Parser.TryStatementExpressionContext ctx) {
    processTryStatementExpression.accept(ctx);
    super.exitTryStatementExpression(ctx);
  }

  @Override
  public void exitInstructionBlock(final EK9Parser.InstructionBlockContext ctx) {
    checkInstructionBlockVariables.accept(ctx);
    super.exitInstructionBlock(ctx);
  }

  @Override
  public void exitObjectAccessStart(final EK9Parser.ObjectAccessStartContext ctx) {
    processObjectAccessStartOrError.accept(ctx);
    super.exitObjectAccessStart(ctx);
  }

  @Override
  public void exitObjectAccessExpression(final EK9Parser.ObjectAccessExpressionContext ctx) {
    processObjectAccessExpressionOrError.accept(ctx);
    super.exitObjectAccessExpression(ctx);
  }

  @Override
  public void exitVariableDeclaration(final EK9Parser.VariableDeclarationContext ctx) {
    checkVariableAssignment.accept(ctx);
    super.exitVariableDeclaration(ctx);
  }

  @Override
  public void exitVariableOnlyDeclaration(final EK9Parser.VariableOnlyDeclarationContext ctx) {
    checkVariableOnlyDeclaration.accept(ctx);
    super.exitVariableOnlyDeclaration(ctx);
  }

  @Override
  public void exitStreamCat(final EK9Parser.StreamCatContext ctx) {
    processStreamCat.accept(ctx);
    super.exitStreamCat(ctx);
  }

  @Override
  public void exitStreamFor(final EK9Parser.StreamForContext ctx) {
    processStreamFor.accept(ctx);
    super.exitStreamFor(ctx);
  }

  @Override
  public void exitPipelinePart(final EK9Parser.PipelinePartContext ctx) {
    processPipelinePart.accept(ctx);
    super.exitPipelinePart(ctx);
  }

  @Override
  public void exitStreamStatementTermination(final EK9Parser.StreamStatementTerminationContext ctx) {
    processStreamStatementTermination.accept(ctx);
    super.exitStreamStatementTermination(ctx);
  }

  @Override
  public void exitStreamStatement(final EK9Parser.StreamStatementContext ctx) {
    processStreamStatement.accept(ctx);
    super.exitStreamStatement(ctx);
  }

  @Override
  public void exitStreamExpressionTermination(final EK9Parser.StreamExpressionTerminationContext ctx) {
    processStreamExpressionTermination.accept(ctx);
    super.exitStreamExpressionTermination(ctx);
  }

  @Override
  public void exitStreamExpression(final EK9Parser.StreamExpressionContext ctx) {
    processStreamExpression.accept(ctx);
    super.exitStreamExpression(ctx);
  }
}
