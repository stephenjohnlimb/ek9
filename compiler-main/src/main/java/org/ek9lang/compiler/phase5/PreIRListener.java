package org.ek9lang.compiler.phase5;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ScopeStackConsistencyListener;

/**
 * Designed to do additional logic checks once everything has been resolved.
 * One of the main things it does it check that variables are 'typed', but this extends to expressions as well.
 * Initially focused on ensuring that uninitialised variables are initialised before being used.
 * But may extend to other forms of checks. For example booleans set to literals and never altered and used in an 'if'.
 * This is designed to try and catch as much as possible before creating an Intermediate Representation and doing
 * further checks.
 */
final class PreIRListener extends ScopeStackConsistencyListener {

  private final ComplexityCounter complexityCounter = new ComplexityCounter();
  private final NestingDepthCounter nestingCounter = new NestingDepthCounter();
  private final FormOfComparator formOfComparator = new FormOfComparator();
  private final FormOfBooleanLogic formOfBooleanLogic;
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
  private final AcceptableArgumentComplexityOrError acceptableArgumentComplexityOrError;
  private final IdentifierAsPropertyOrError processIdentifierAsProperty;
  private final AcceptableConstructComplexityOrError acceptableConstructComplexityOrError;
  private final AcceptableNestingDepthOrError acceptableNestingOrError;
  private final SuitablePropertyInitialisationOrError suitablePropertyInitialisationOrError;
  private final ObjectAccessExpressionValidOrError objectAccessExpressionValidOrError;
  private final IfBlockSafeGenericAccessMarker ifBlockSafeGenericAccessMarker;
  private final SwitchBlockSafeGenericAccessMarker switchBlockSafeGenericAccessMarker;
  private final TernaryBlockSafeGenericAccessMarker ternaryBlockSafeGenericAccessMarker;
  private final WhileLoopSafeGenericAccessMarker whileLoopSafeGenericAccessMarker;
  private final ForLoopSafeGenericAccessMarker forLoopSafeGenericAccessMarker;
  private final TryBlockSafeGenericAccessMarker tryBlockSafeGenericAccessMarker;
  private final SymbolHasTypeOrError symbolHasTypeOrError;

  PreIRListener(final ParsedModule parsedModule) {

    super(parsedModule);
    final var errorListener = parsedModule.getSource().getErrorListener();

    this.formOfBooleanLogic =
        new FormOfBooleanLogic(symbolsAndScopes, parsedModule.getEk9Types());

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
    this.acceptableArgumentComplexityOrError =
        new AcceptableArgumentComplexityOrError(complexityCounter, errorListener);
    this.processIdentifierAsProperty =
        new IdentifierAsPropertyOrError(symbolsAndScopes, errorListener);
    this.acceptableConstructComplexityOrError =
        new AcceptableConstructComplexityOrError(symbolsAndScopes, errorListener, complexityCounter);
    this.acceptableNestingOrError =
        new AcceptableNestingDepthOrError(symbolsAndScopes, errorListener, nestingCounter);
    this.suitablePropertyInitialisationOrError =
        new SuitablePropertyInitialisationOrError(symbolsAndScopes, errorListener);
    this.objectAccessExpressionValidOrError =
        new ObjectAccessExpressionValidOrError(symbolsAndScopes, errorListener);
    this.ifBlockSafeGenericAccessMarker =
        new IfBlockSafeGenericAccessMarker(symbolsAndScopes, errorListener);
    this.switchBlockSafeGenericAccessMarker =
        new SwitchBlockSafeGenericAccessMarker(symbolsAndScopes, errorListener);
    this.ternaryBlockSafeGenericAccessMarker =
        new TernaryBlockSafeGenericAccessMarker(symbolsAndScopes, errorListener);
    this.whileLoopSafeGenericAccessMarker =
        new WhileLoopSafeGenericAccessMarker(symbolsAndScopes, errorListener);
    this.forLoopSafeGenericAccessMarker =
        new ForLoopSafeGenericAccessMarker(symbolsAndScopes, errorListener);
    this.tryBlockSafeGenericAccessMarker =
        new TryBlockSafeGenericAccessMarker(symbolsAndScopes, errorListener);
    this.symbolHasTypeOrError =
        new SymbolHasTypeOrError(symbolsAndScopes, errorListener);
  }

  @Override
  public void enterServiceDeclaration(final EK9Parser.ServiceDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    //Services are complex by nature.
    complexityCounter.push(2);
    super.enterServiceDeclaration(ctx);
  }

  @Override
  public void exitServiceDeclaration(final EK9Parser.ServiceDeclarationContext ctx) {

    acceptableConstructComplexityOrError.andThen(suitablePropertyInitialisationOrError).accept(ctx);
    super.exitServiceDeclaration(ctx);
  }

  @Override
  public void enterRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.push(1);
    super.enterRecordDeclaration(ctx);
  }

  @Override
  public void exitRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {

    acceptableConstructComplexityOrError.andThen(suitablePropertyInitialisationOrError).accept(ctx);
    super.exitRecordDeclaration(ctx);
  }

  @Override
  public void enterTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.push(1);
    super.enterTraitDeclaration(ctx);
  }

  @Override
  public void exitTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    acceptableConstructComplexityOrError.accept(ctx);
    super.exitTraitDeclaration(ctx);
  }

  @Override
  public void enterClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.push(1);
    super.enterClassDeclaration(ctx);
  }

  @Override
  public void exitClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    acceptableConstructComplexityOrError.andThen(suitablePropertyInitialisationOrError).accept(ctx);
    super.exitClassDeclaration(ctx);
  }

  @Override
  public void enterComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    //Components are also complex by nature with all the injection parts.
    complexityCounter.push(2);
    super.enterComponentDeclaration(ctx);
  }

  @Override
  public void exitComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {

    acceptableConstructComplexityOrError.andThen(suitablePropertyInitialisationOrError).accept(ctx);
    super.exitComponentDeclaration(ctx);
  }

  @Override
  public void enterApplicationDeclaration(final EK9Parser.ApplicationDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    //Applications are also quite complex in their nature - working with Components.
    complexityCounter.push(2);
    super.enterApplicationDeclaration(ctx);
  }

  @Override
  public void exitApplicationDeclaration(final EK9Parser.ApplicationDeclarationContext ctx) {

    acceptableConstructComplexityOrError.accept(ctx);
    super.exitApplicationDeclaration(ctx);
  }

  @Override
  public void enterDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    //These are also complex, because they inline a class definition.
    complexityCounter.push(2);
    super.enterDynamicClassDeclaration(ctx);
  }

  @Override
  public void exitDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    acceptableConstructComplexityOrError.andThen(suitablePropertyInitialisationOrError).accept(ctx);
    super.exitDynamicClassDeclaration(ctx);
  }

  @Override
  public void enterFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.push(1);
    nestingCounter.pushScope();
    super.enterFunctionDeclaration(ctx);
  }

  @Override
  public void exitFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    functionOrError.andThen(acceptableConstructComplexityOrError).andThen(acceptableNestingOrError).accept(ctx);
    super.exitFunctionDeclaration(ctx);
  }

  @Override
  public void enterMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.push(1);
    nestingCounter.pushScope();
    super.enterMethodDeclaration(ctx);
  }

  @Override
  public void exitMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    methodOrError.andThen(acceptableConstructComplexityOrError).andThen(acceptableNestingOrError).accept(ctx);
    super.exitMethodDeclaration(ctx);
  }

  @Override
  public void enterOperatorDeclaration(final EK9Parser.OperatorDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.push(1);
    nestingCounter.pushScope();
    super.enterOperatorDeclaration(ctx);
  }

  @Override
  public void exitOperatorDeclaration(final EK9Parser.OperatorDeclarationContext ctx) {

    operatorOrError.andThen(acceptableConstructComplexityOrError).andThen(acceptableNestingOrError).accept(ctx);
    super.exitOperatorDeclaration(ctx);
  }

  @Override
  public void enterServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.push(1);
    nestingCounter.pushScope();
    super.enterServiceOperationDeclaration(ctx);
  }

  @Override
  public void exitServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    serviceOperationOrError.andThen(acceptableConstructComplexityOrError).andThen(acceptableNestingOrError).accept(ctx);
    super.exitServiceOperationDeclaration(ctx);
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

    symbolHasTypeOrError.accept(ctx);

    //Quite complex to understand.
    complexityCounter.push(2);
    nestingCounter.pushScope();
    processDynamicFunctionDeclarationEntry.accept(ctx);
    super.enterDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void exitDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    dynamicFunctionOrError.andThen(acceptableConstructComplexityOrError).andThen(acceptableNestingOrError).accept(ctx);

    super.exitDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void enterTextDeclaration(final EK9Parser.TextDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterTextDeclaration(ctx);
  }

  @Override
  public void enterTextBodyDeclaration(final EK9Parser.TextBodyDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterTextBodyDeclaration(ctx);
  }

  @Override
  public void enterTypeDeclaration(final EK9Parser.TypeDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterTypeDeclaration(ctx);
  }

  @Override
  public void enterParameterisedType(final EK9Parser.ParameterisedTypeContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterParameterisedType(ctx);
  }

  @Override
  public void enterTraitReference(final EK9Parser.TraitReferenceContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterTraitReference(ctx);
  }

  @Override
  public void enterVariableOnlyDeclaration(final EK9Parser.VariableOnlyDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    if (ctx.QUESTION() != null) {
      //So declaring a variable that is unset, while it may be necessary adds to complexity and
      //risk as now all paths that use that variable must either check it or assign to it.
      complexityCounter.incrementComplexity();
    }

    variableOnlyOrError.accept(ctx);

    super.enterVariableOnlyDeclaration(ctx);
  }

  @Override
  public void enterVariableDeclaration(final EK9Parser.VariableDeclarationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterVariableDeclaration(ctx);
  }

  @Override
  public void enterTypeDef(final EK9Parser.TypeDefContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterTypeDef(ctx);
  }

  @Override
  public void enterObjectAccessStart(final EK9Parser.ObjectAccessStartContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterObjectAccessStart(ctx);
  }

  @Override
  public void enterObjectAccessExpression(EK9Parser.ObjectAccessExpressionContext ctx) {

    symbolHasTypeOrError.accept(ctx);
    objectAccessExpressionValidOrError.accept(ctx);

    super.enterObjectAccessExpression(ctx);
  }

  @Override
  public void enterObjectAccess(final EK9Parser.ObjectAccessContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterObjectAccess(ctx);
  }

  @Override
  public void enterObjectAccessType(final EK9Parser.ObjectAccessTypeContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterObjectAccessType(ctx);
  }

  @Override
  public void enterAssignmentStatement(final EK9Parser.AssignmentStatementContext ctx) {

    if (ctx.ASSIGN_UNSET() != null) {
      //Again normally used in a pure context, but can be useful for conditional assignment when lhs is unset.
      //Basically this is like an if the lhs is unset then assign the rhs to it - i.e. adds complexity of thought.
      complexityCounter.incrementComplexity();
    }

    assignmentStatementOrError.accept(ctx);

    super.enterAssignmentStatement(ctx);
  }

  @Override
  public void enterGuardExpression(final EK9Parser.GuardExpressionContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.incrementComplexity();
    processGuardExpression.accept(ctx);

    super.enterGuardExpression(ctx);
  }


  @Override
  public void enterForStatementExpression(final EK9Parser.ForStatementExpressionContext ctx) {

    symbolHasTypeOrError.accept(ctx);
    forLoopSafeGenericAccessMarker.accept(ctx);
    nestingCounter.enterNesting();
    super.enterForStatementExpression(ctx);
  }

  @Override
  public void enterAssignmentExpression(final EK9Parser.AssignmentExpressionContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterAssignmentExpression(ctx);
  }

  @Override
  public void enterCall(final EK9Parser.CallContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterCall(ctx);
  }

  @Override
  public void enterOperationCall(final EK9Parser.OperationCallContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterOperationCall(ctx);
  }

  @Override
  public void enterCaseExpression(final EK9Parser.CaseExpressionContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.incrementComplexity();
    super.enterCaseExpression(ctx);
  }

  @Override
  public void enterThrowStatement(final EK9Parser.ThrowStatementContext ctx) {

    //Introducing throws adds complexity.
    complexityCounter.incrementComplexity(2);
    super.enterThrowStatement(ctx);
  }

  @Override
  public void enterTryStatementExpression(final EK9Parser.TryStatementExpressionContext ctx) {

    symbolHasTypeOrError.accept(ctx);
    tryBlockSafeGenericAccessMarker.accept(ctx);
    complexityCounter.incrementComplexity();
    nestingCounter.enterNesting();
    super.enterTryStatementExpression(ctx);
  }

  @Override
  public void enterCatchStatementExpression(final EK9Parser.CatchStatementExpressionContext ctx) {

    complexityCounter.incrementComplexity();
    super.enterCatchStatementExpression(ctx);
  }

  @Override
  public void enterFinallyStatementExpression(final EK9Parser.FinallyStatementExpressionContext ctx) {

    complexityCounter.incrementComplexity();
    super.enterFinallyStatementExpression(ctx);
  }

  @Override
  public void enterForLoop(final EK9Parser.ForLoopContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.incrementComplexity();
    super.enterForLoop(ctx);
  }

  @Override
  public void enterForRange(final EK9Parser.ForRangeContext ctx) {

    //This does make the for loop harder to understand - hence more complex
    if (ctx.BY() != null) {
      complexityCounter.incrementComplexity(2);
    } else {
      complexityCounter.incrementComplexity();
    }
    super.enterForRange(ctx);
  }

  @Override
  public void enterStreamSource(final EK9Parser.StreamSourceContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterStreamSource(ctx);
  }

  @Override
  public void enterPipelinePart(final EK9Parser.PipelinePartContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.incrementComplexity();
    super.enterPipelinePart(ctx);
  }

  @Override
  public void enterStreamExpression(final EK9Parser.StreamExpressionContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterStreamExpression(ctx);
  }

  @Override
  public void enterStreamExpressionTermination(final EK9Parser.StreamExpressionTerminationContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.incrementComplexity();
    super.enterStreamExpressionTermination(ctx);
  }

  @Override
  public void enterStreamCat(final EK9Parser.StreamCatContext ctx) {

    symbolHasTypeOrError.accept(ctx);
    complexityCounter.incrementComplexity();
    super.enterStreamCat(ctx);
  }

  @Override
  public void enterStreamFor(final EK9Parser.StreamForContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterStreamFor(ctx);
  }

  @Override
  public void enterWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    complexityCounter.incrementComplexity();
    whileLoopSafeGenericAccessMarker.accept(ctx);
    nestingCounter.enterNesting();

    super.enterWhileStatementExpression(ctx);
  }

  @Override
  public void enterExpressionParam(final EK9Parser.ExpressionParamContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterExpressionParam(ctx);
  }

  @Override
  public void enterPrimaryReference(final EK9Parser.PrimaryReferenceContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterPrimaryReference(ctx);
  }

  @Override
  public void enterPrimary(final EK9Parser.PrimaryContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterPrimary(ctx);
  }

  @Override
  public void enterDeclareArgumentParam(EK9Parser.DeclareArgumentParamContext ctx) {

    if (ctx.variableDeclaration() != null && !ctx.variableDeclaration().isEmpty()) {
      acceptableArgumentComplexityOrError.accept(ctx.start, ctx.variableDeclaration().size());
    }
    super.enterDeclareArgumentParam(ctx);
  }

  @Override
  public void enterArgumentParam(EK9Parser.ArgumentParamContext ctx) {
    if (ctx.variableOnlyDeclaration() != null && !ctx.variableOnlyDeclaration().isEmpty()) {
      acceptableArgumentComplexityOrError.accept(ctx.RIGHT_ARROW().getSymbol(), ctx.variableOnlyDeclaration().size());
    }

    super.enterArgumentParam(ctx);
  }

  @Override
  public void enterIdentifier(final EK9Parser.IdentifierContext ctx) {

    //It may or may not be an aggregate property but this consumer will determine that.
    processIdentifierAsProperty.accept(ctx);

    super.enterIdentifier(ctx);
  }

  @Override
  public void exitIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {

    identifierReferenceOrError.accept(ctx);

    super.exitIdentifierReference(ctx);
  }

  @Override
  public void enterIfControlBlock(final EK9Parser.IfControlBlockContext ctx) {
    complexityCounter.incrementComplexity();
    ifBlockSafeGenericAccessMarker.accept(ctx);
    super.enterIfControlBlock(ctx);
  }

  @Override
  public void enterExpression(final EK9Parser.ExpressionContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    //This means that this is a ternary
    if (ctx.control != null && ctx.ternary != null) {
      ternaryBlockSafeGenericAccessMarker.accept(ctx);
    }
  }

  @Override
  public void enterTernaryPart(final EK9Parser.TernaryPartContext ctx) {

    symbolHasTypeOrError.accept(ctx);

    super.enterTernaryPart(ctx);
  }

  @Override
  public void enterIfStatement(final EK9Parser.IfStatementContext ctx) {

    nestingCounter.enterNesting();
    super.enterIfStatement(ctx);
  }

  @Override
  public void exitIfStatement(final EK9Parser.IfStatementContext ctx) {

    //Note that exit to pop stack first.
    super.exitIfStatement(ctx);

    nestingCounter.exitNesting();
    ifStatementOrError.accept(ctx);
  }

  @Override
  public void enterSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {

    symbolHasTypeOrError.accept(ctx);
    switchBlockSafeGenericAccessMarker.accept(ctx);
    nestingCounter.enterNesting();
    super.enterSwitchStatementExpression(ctx);
  }

  @Override
  public void exitSwitchStatementExpression(final EK9Parser.SwitchStatementExpressionContext ctx) {

    super.exitSwitchStatementExpression(ctx);

    nestingCounter.exitNesting();
    switchStatementOrError.accept(ctx);
  }

  @Override
  public void exitTryStatementExpression(final EK9Parser.TryStatementExpressionContext ctx) {

    super.exitTryStatementExpression(ctx);

    nestingCounter.exitNesting();
    tryStatementOrError.accept(ctx);
  }

  @Override
  public void exitForStatementExpression(final EK9Parser.ForStatementExpressionContext ctx) {

    super.exitForStatementExpression(ctx);

    nestingCounter.exitNesting();
    forStatementOrError.accept(ctx);
  }

  @Override
  public void exitWhileStatementExpression(final EK9Parser.WhileStatementExpressionContext ctx) {

    super.exitWhileStatementExpression(ctx);

    nestingCounter.exitNesting();
    whileStatementOrError.accept(ctx);
  }

  @Override
  public void exitExpression(final EK9Parser.ExpressionContext ctx) {
    if (formOfComparator.test(ctx)) {
      complexityCounter.incrementComplexity();
    }
    //Boolean and/or creates branching (short-circuit), so adds complexity
    if (formOfBooleanLogic.test(ctx)) {
      complexityCounter.incrementComplexity();
    }
    super.exitExpression(ctx);
  }
}
