package org.ek9lang.compiler.phase3;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.CaptureScope;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * <p>
 * This is a really critical point, because this attempts to ensure that ANY expression
 * results in a symbol that has been 'typed'.
 * </p>
 * <p>
 * But it also has to 'resolve' types or 'generic types' and for inferred declarations must
 * work out what the types are (hence previous expressions must now be 'typed').
 * It will also trigger the creations of new parameterised types, from generic types and type arguments,
 * but 'just in time'.
 * </p>
 * <p>
 * Due to the ways tha antlr employs listeners, breaking up the processing of listener events in to
 * separate listeners in a type hierarchy seems to make most sense.
 * But the bulk of any real actual processing is pulled out to separate functions and classes.
 * So that these large and quite complex 'flows' can just focus on the event cycles and manage the scope stacks.
 * </p>
 */
final class ResolveDefineInferredTypeListener extends ExpressionsListener {

  private final DynamicCaptureAndDefinition dynamicCaptureAndDefinition;
  private final CheckPossibleFieldDelegate checkPossibleFieldDelegate;
  private final CheckPropertyNames checkPropertyNames;
  private final CheckDefaultOperators checkDefaultOperators;
  private final CheckMethodOverrides checkMethodOverrides;
  private final CheckMethodOverrides checkDynamicClassMethodOverrides;
  private final CheckFunctionOverrides checkFunctionOverrides;
  private final AutoMatchSuperFunctionSignature autoMatchSuperFunctionSignature;
  private final CheckForDynamicFunctionBody checkForDynamicFunctionBody;
  private final CheckReturn checkDynamicFunctionReturn;
  private final CheckReturn checkFunctionReturn;
  private final CheckMethodReturn checkMethodReturn;
  private final CheckAllTextBodiesPresent checkAllTextBodiesPresent;

  private final CheckServiceRegistration checkServiceRegistration;

  private final CheckTypeConstraint checkTypeConstraint;

  /**
   * Create a new instance to define or resolve inferred types.
   */
  ResolveDefineInferredTypeListener(ParsedModule parsedModule) {
    super(parsedModule);
    this.dynamicCaptureAndDefinition =
        new DynamicCaptureAndDefinition(symbolAndScopeManagement, errorListener, symbolFactory);

    this.checkPossibleFieldDelegate = new CheckPossibleFieldDelegate(symbolAndScopeManagement, errorListener);
    this.checkPropertyNames = new CheckPropertyNames(symbolAndScopeManagement, errorListener);
    this.checkDefaultOperators = new CheckDefaultOperators(symbolAndScopeManagement, errorListener);
    this.checkMethodOverrides = new CheckMethodOverrides(symbolAndScopeManagement,
        errorListener, ErrorListener.SemanticClassification.NOT_MARKED_ABSTRACT_BUT_IS_ABSTRACT);
    this.checkDynamicClassMethodOverrides = new CheckMethodOverrides(symbolAndScopeManagement,
        errorListener, ErrorListener.SemanticClassification.DYNAMIC_CLASS_MUST_IMPLEMENT_ABSTRACTS);
    this.checkFunctionOverrides = new CheckFunctionOverrides(symbolAndScopeManagement, errorListener);
    this.checkForDynamicFunctionBody = new CheckForDynamicFunctionBody(symbolAndScopeManagement, errorListener);
    this.autoMatchSuperFunctionSignature = new AutoMatchSuperFunctionSignature(symbolAndScopeManagement, errorListener);
    this.checkDynamicFunctionReturn = new CheckReturn(true, symbolAndScopeManagement, errorListener);
    this.checkFunctionReturn = new CheckReturn(false, symbolAndScopeManagement, errorListener);
    this.checkMethodReturn = new CheckMethodReturn(symbolAndScopeManagement, errorListener);
    this.checkAllTextBodiesPresent = new CheckAllTextBodiesPresent(symbolAndScopeManagement, errorListener);
    this.checkServiceRegistration = new CheckServiceRegistration(symbolAndScopeManagement, errorListener);
    this.checkTypeConstraint = new CheckTypeConstraint(symbolAndScopeManagement, symbolFactory, errorListener);
  }

  @Override
  public void enterDynamicVariableCapture(EK9Parser.DynamicVariableCaptureContext ctx) {
    CaptureScope scope = (CaptureScope) symbolAndScopeManagement.getRecordedScope(ctx);
    scope.setOpenToEnclosingScope(true);
    super.enterDynamicVariableCapture(ctx);
  }

  @Override
  public void exitDynamicVariableCapture(EK9Parser.DynamicVariableCaptureContext ctx) {
    CaptureScope scope = (CaptureScope) symbolAndScopeManagement.getRecordedScope(ctx);
    dynamicCaptureAndDefinition.accept(ctx);
    scope.setOpenToEnclosingScope(false);

    super.exitDynamicVariableCapture(ctx);
  }

  @Override
  public void exitAggregateProperty(EK9Parser.AggregatePropertyContext ctx) {
    var field = symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkPossibleFieldDelegate.accept(field);

    super.exitAggregateProperty(ctx);
  }

  @Override
  public void exitRecordDeclaration(EK9Parser.RecordDeclarationContext ctx) {
    var symbol = (AggregateSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkDefaultOperators
        .andThen(checkPropertyNames)
        .andThen(checkMethodOverrides)
        .andThen(checkMethodReturn)
        .accept(symbol);
    super.exitRecordDeclaration(ctx);
  }

  @Override
  public void exitClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkDefaultOperators.andThen(checkMethodOverrides).andThen(checkMethodReturn).accept(symbol);
    super.exitClassDeclaration(ctx);
  }

  @Override
  public void exitDynamicClassDeclaration(EK9Parser.DynamicClassDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkDefaultOperators.andThen(checkDynamicClassMethodOverrides).andThen(checkMethodReturn).accept(symbol);

    super.exitDynamicClassDeclaration(ctx);
  }

  @Override
  public void exitTraitDeclaration(EK9Parser.TraitDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkMethodOverrides.andThen(checkMethodReturn).accept(symbol);
    super.exitTraitDeclaration(ctx);
  }

  @Override
  public void exitComponentDeclaration(EK9Parser.ComponentDeclarationContext ctx) {
    var symbol = (AggregateSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkDefaultOperators.andThen(checkMethodOverrides).andThen(checkMethodReturn).accept(symbol);
    super.exitComponentDeclaration(ctx);
  }

  @Override
  public void exitServiceDeclaration(EK9Parser.ServiceDeclarationContext ctx) {
    var symbol = (AggregateSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkMethodReturn.accept(symbol);
    super.exitServiceDeclaration(ctx);
  }

  @Override
  public void exitFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    var symbol = (FunctionSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkFunctionOverrides.accept(symbol);
    checkFunctionReturn.accept(symbol, symbol.getReturningSymbol());
    super.exitFunctionDeclaration(ctx);
  }

  @Override
  public void enterDynamicFunctionDeclaration(EK9Parser.DynamicFunctionDeclarationContext ctx) {
    var symbol = (FunctionSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    //Automatically populate the function signature and return for a dynamic function
    autoMatchSuperFunctionSignature.accept(symbol);
    //Now just check it, should be fine - maybe remove in the future.
    checkFunctionOverrides.accept(symbol);

    super.enterDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void exitDynamicFunctionDeclaration(EK9Parser.DynamicFunctionDeclarationContext ctx) {
    var symbol = (FunctionSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    //But check that if the super had no implementation this dynamic function does.
    checkForDynamicFunctionBody.accept(ctx);
    checkDynamicFunctionReturn.accept(symbol, symbol.getReturningSymbol());

    super.exitDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void exitTextDeclaration(EK9Parser.TextDeclarationContext ctx) {
    //At this point, all the text declarations and their text bodies will have been defined.
    //But also the 'super' with all the methods will have also been defined.
    //This is the point where we can check that this specific text declaration has all the same methods as the super.
    var textAggregate = (AggregateSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (textAggregate != null) {
      //Only check if valid - might have been a duplicate.
      checkAllTextBodiesPresent.accept(textAggregate);
    }
    super.exitTextDeclaration(ctx);
  }

  @Override
  public void exitTypeDeclaration(EK9Parser.TypeDeclarationContext ctx) {
    var aggregateSymbol = (AggregateSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (aggregateSymbol != null && ctx.constrainDeclaration() != null) {
      //Then it is constrained type, so we need to check that the appropriate operators
      //used in these constraints actually exist.
      checkTypeConstraint.accept(aggregateSymbol, ctx.constrainDeclaration());

    }
    super.exitTypeDeclaration(ctx);
  }

  @Override
  public void exitRegisterStatement(EK9Parser.RegisterStatementContext ctx) {
    checkServiceRegistration.accept(ctx);
    super.exitRegisterStatement(ctx);
  }
}
