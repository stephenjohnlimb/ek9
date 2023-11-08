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
  private final CheckForRange checkForRange;
  private final CheckVariableAssignment checkVariableAssignment;
  private final CheckVariableOnlyDeclaration checkVariableOnlyDeclaration;
  private final CheckPipelinePart checkPipelinePart;
  private final ProcessObjectAccessStartOrError processObjectAccessStartOrError;
  private final ProcessObjectAccessExpressionOrError processObjectAccessExpressionOrError;

  protected ExpressionsListener(ParsedModule parsedModule) {
    super(parsedModule);

    this.symbolFactory = new SymbolFactory(parsedModule);

    this.errorListener = parsedModule.getSource().getErrorListener();

    this.processValidThisOrSuper = new ProcessValidThisOrSuper(symbolAndScopeManagement, symbolFactory, errorListener);

    this.processValidPrimary = new ProcessValidPrimary(symbolAndScopeManagement, errorListener);

    this.processValidIdentifierReference = new ProcessValidIdentifierReference(symbolAndScopeManagement, errorListener);

    this.checkValidStatement = new CheckValidStatement(symbolAndScopeManagement, errorListener);

    this.checkValidExpression = new CheckValidExpression(symbolAndScopeManagement, symbolFactory, errorListener);

    this.checkInstructionBlockVariables = new CheckInstructionBlockVariables(symbolAndScopeManagement, errorListener);

    this.processAssignmentExpression = new ProcessAssignmentExpression(symbolAndScopeManagement, errorListener);

    this.processAssignmentStatement = new ProcessAssignmentStatement(symbolAndScopeManagement, errorListener);

    this.checkValidCall = new CheckValidCall(symbolAndScopeManagement, symbolFactory, errorListener);

    this.processAndTypeList = new ProcessAndTypeList(symbolAndScopeManagement, symbolFactory, errorListener);

    this.processAndTypeDict = new ProcessAndTypeDict(symbolAndScopeManagement, symbolFactory, errorListener);

    this.checkPipelinePart = new CheckPipelinePart(symbolAndScopeManagement, errorListener);

    this.processRange = new ProcessRange(symbolAndScopeManagement, symbolFactory, errorListener);

    this.checkForRange = new CheckForRange(symbolAndScopeManagement, errorListener);

    this.checkVariableAssignment = new CheckVariableAssignment(symbolAndScopeManagement, errorListener);

    this.checkVariableOnlyDeclaration = new CheckVariableOnlyDeclaration(symbolAndScopeManagement, errorListener);

    this.processObjectAccessStartOrError = new ProcessObjectAccessStartOrError(symbolAndScopeManagement, errorListener);
    this.processObjectAccessExpressionOrError =
        new ProcessObjectAccessExpressionOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void exitIdentifierReference(EK9Parser.IdentifierReferenceContext ctx) {
    processValidIdentifierReference.apply(ctx);
    super.exitIdentifierReference(ctx);
  }

  @Override
  public void exitTraitReference(EK9Parser.TraitReferenceContext ctx) {
    var traitReference = symbolAndScopeManagement.getRecordedSymbol(ctx.identifierReference());
    if (traitReference != null) {
      symbolAndScopeManagement.recordSymbol(traitReference, ctx);
    }
    super.exitTraitReference(ctx);
  }

  @Override
  public void exitPrimaryReference(EK9Parser.PrimaryReferenceContext ctx) {
    //Here we are modelling 'this' or 'super', idea is to set type or issue errors.
    processValidThisOrSuper.accept(ctx);
    super.exitPrimaryReference(ctx);
  }

  @Override
  public void exitPrimary(EK9Parser.PrimaryContext ctx) {
    processValidPrimary.accept(ctx);
    super.exitPrimary(ctx);
  }

  @Override
  public void exitCall(EK9Parser.CallContext ctx) {
    checkValidCall.accept(ctx);
    super.exitCall(ctx);
  }

  @Override
  public void exitList(EK9Parser.ListContext ctx) {
    processAndTypeList.accept(ctx);
    super.exitList(ctx);
  }

  @Override
  public void exitDict(EK9Parser.DictContext ctx) {
    processAndTypeDict.accept(ctx);
    super.exitDict(ctx);
  }

  @Override
  public void exitRange(EK9Parser.RangeContext ctx) {
    processRange.accept(ctx);
    super.exitRange(ctx);
  }

  @Override
  public void exitForRange(EK9Parser.ForRangeContext ctx) {
    checkForRange.accept(ctx);
    super.exitForRange(ctx);
  }

  @Override
  public void exitStatement(EK9Parser.StatementContext ctx) {
    checkValidStatement.accept(ctx);
    super.exitStatement(ctx);
  }

  @Override
  public void exitExpression(EK9Parser.ExpressionContext ctx) {
    checkValidExpression.accept(ctx);
    super.exitExpression(ctx);
  }

  @Override
  public void exitAssignmentExpression(EK9Parser.AssignmentExpressionContext ctx) {
    processAssignmentExpression.accept(ctx);
    super.exitAssignmentExpression(ctx);
  }

  @Override
  public void exitAssignmentStatement(EK9Parser.AssignmentStatementContext ctx) {
    processAssignmentStatement.accept(ctx);
    super.exitAssignmentStatement(ctx);
  }

  @Override
  public void exitInstructionBlock(EK9Parser.InstructionBlockContext ctx) {
    checkInstructionBlockVariables.accept(ctx);
    super.exitInstructionBlock(ctx);
  }

  @Override
  public void exitObjectAccessStart(EK9Parser.ObjectAccessStartContext ctx) {
    processObjectAccessStartOrError.accept(ctx);
    super.exitObjectAccessStart(ctx);
  }

  @Override
  public void exitObjectAccessExpression(EK9Parser.ObjectAccessExpressionContext ctx) {
    processObjectAccessExpressionOrError.accept(ctx);
    super.exitObjectAccessExpression(ctx);
  }

  @Override
  public void exitVariableDeclaration(EK9Parser.VariableDeclarationContext ctx) {
    checkVariableAssignment.accept(ctx);
    super.exitVariableDeclaration(ctx);
  }

  @Override
  public void exitVariableOnlyDeclaration(EK9Parser.VariableOnlyDeclarationContext ctx) {
    checkVariableOnlyDeclaration.accept(ctx);
    super.exitVariableOnlyDeclaration(ctx);
  }

  @Override
  public void exitPipelinePart(EK9Parser.PipelinePartContext ctx) {
    checkPipelinePart.accept(ctx);

    super.exitPipelinePart(ctx);
  }
}
