package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DUPLICATE_PROPERTY_FIELD;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.CaptureScope;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.core.CompilerException;

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
  private final CheckPropertyNames checkPropertyNames;
  private final CheckDefaultOperators checkDefaultOperators;
  private final CheckMethodOverrides checkMethodOverrides;
  private final CheckMethodOverrides checkDynamicClassMethodOverrides;
  private final CheckConflictingMethods checkNoConflictingMethods;
  private final CheckFunctionOverrides checkFunctionOverrides;
  private final AutoMatchSuperFunctionSignature autoMatchSuperFunctionSignature;
  private final CheckForDynamicFunctionBody checkForDynamicFunctionBody;
  private final CheckAllTextBodiesPresent checkAllTextBodiesPresent;
  private final CheckServiceRegistration checkServiceRegistration;
  private final ProcessTypeConstraint processTypeConstraint;
  private final AugmentAggregateWithTraitMethods augmentAggregateWithTraitMethods;
  private final ResolveByTraitVariables resolveByTraitVariables;
  private final CheckNoTraitByVariables checkNoTraitByVariablesOrError;

  private final ProcessEnumeratedType processEnumeratedType;

  /**
   * Create a new instance to define or resolve inferred types.
   */
  ResolveDefineInferredTypeListener(ParsedModule parsedModule) {
    super(parsedModule);

    this.dynamicCaptureAndDefinition =
        new DynamicCaptureAndDefinition(symbolAndScopeManagement, errorListener, symbolFactory);
    this.checkPropertyNames =
        new CheckPropertyNames(symbolAndScopeManagement, errorListener, DUPLICATE_PROPERTY_FIELD);
    this.checkDefaultOperators =
        new CheckDefaultOperators(symbolAndScopeManagement, errorListener);
    this.checkMethodOverrides =
        new CheckMethodOverrides(symbolAndScopeManagement,
            errorListener, ErrorListener.SemanticClassification.NOT_MARKED_ABSTRACT_BUT_IS_ABSTRACT);
    this.checkDynamicClassMethodOverrides =
        new CheckMethodOverrides(symbolAndScopeManagement,
            errorListener, ErrorListener.SemanticClassification.DYNAMIC_CLASS_MUST_IMPLEMENT_ABSTRACTS);
    this.checkNoConflictingMethods =
        new CheckConflictingMethods(symbolAndScopeManagement, errorListener);
    this.checkFunctionOverrides =
        new CheckFunctionOverrides(symbolAndScopeManagement, errorListener);
    this.checkForDynamicFunctionBody =
        new CheckForDynamicFunctionBody(symbolAndScopeManagement, errorListener);
    this.autoMatchSuperFunctionSignature =
        new AutoMatchSuperFunctionSignature(symbolAndScopeManagement, errorListener);
    this.checkAllTextBodiesPresent =
        new CheckAllTextBodiesPresent(symbolAndScopeManagement, errorListener);
    this.checkServiceRegistration =
        new CheckServiceRegistration(symbolAndScopeManagement, errorListener);
    this.processTypeConstraint =
        new ProcessTypeConstraint(symbolAndScopeManagement, symbolFactory, errorListener);
    this.augmentAggregateWithTraitMethods =
        new AugmentAggregateWithTraitMethods(symbolAndScopeManagement, errorListener);
    this.resolveByTraitVariables =
        new ResolveByTraitVariables(symbolAndScopeManagement, errorListener);
    this.checkNoTraitByVariablesOrError =
        new CheckNoTraitByVariables(symbolAndScopeManagement, errorListener);
    this.processEnumeratedType =
        new ProcessEnumeratedType(symbolAndScopeManagement, symbolFactory, errorListener);
  }

  @Override
  public void enterParameterisedType(EK9Parser.ParameterisedTypeContext ctx) {
    //So finally at this point - as a parameterized type is explicitly encountered
    //WE MUST re-resolve it and in this FULL_COMPILATION phase it will then get all it's types
    //substituted - unless they have already been substituted.
    var theType = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (theType instanceof PossibleGenericSymbol possibleGenericSymbol) {
      reResolveParameterisedType(possibleGenericSymbol);
    } else {
      throw new CompilerException("Expecting parameterised type to exist.");
    }
    super.enterParameterisedType(ctx);
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
  public void enterTypeDeclaration(EK9Parser.TypeDeclarationContext ctx) {
    var theType = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (theType != null && theType.getGenus().equals(ISymbol.SymbolGenus.CLASS_ENUMERATION)) {
      //We must add the iterator method to this.
      processEnumeratedType.accept(ctx);
    }
    super.enterTypeDeclaration(ctx);
  }

  @Override
  public void exitRecordDeclaration(EK9Parser.RecordDeclarationContext ctx) {
    var symbol = (AggregateSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkDefaultOperators
        .andThen(checkPropertyNames)
        .andThen(checkMethodOverrides)
        .accept(symbol);
    super.exitRecordDeclaration(ctx);
  }

  @Override
  public void enterClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    symbol.getSuperAggregate().ifPresent(this::reResolveParameterisedType);

    //Now we may modify this class definition if it uses 'traits by'
    //We do this in the entry, because on exit (below) we will check all abstract methods implemented.
    if (checkNoConflictingMethods.test(symbol)) {
      augmentAggregateWithTraitMethods.accept(ctx.traitsList(), symbol);
    }
    super.enterClassDeclaration(ctx);
  }

  @Override
  public void exitClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkDefaultOperators
        .andThen(checkMethodOverrides)
        .accept(symbol);

    resolveByTraitVariables.accept(ctx.traitsList(), symbol);
    super.exitClassDeclaration(ctx);
  }

  @Override
  public void enterDynamicClassDeclaration(EK9Parser.DynamicClassDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    symbol.getSuperAggregate().ifPresent(this::reResolveParameterisedType);

    //Now we may modify this class definition if it uses 'traits by'
    //We do this in the entry, because on exit (below) we will check all abstract methods implemented.
    if (checkNoConflictingMethods.test(symbol)) {
      augmentAggregateWithTraitMethods.accept(ctx.traitsList(), symbol);
    }

    super.enterDynamicClassDeclaration(ctx);
  }

  @Override
  public void exitDynamicClassDeclaration(EK9Parser.DynamicClassDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkDefaultOperators
        .andThen(checkDynamicClassMethodOverrides)
        .accept(symbol);

    resolveByTraitVariables.accept(ctx.traitsList(), symbol);

    super.exitDynamicClassDeclaration(ctx);
  }

  @Override
  public void enterTraitDeclaration(EK9Parser.TraitDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkNoConflictingMethods.test(symbol);
    checkNoTraitByVariablesOrError.accept(ctx.traitsList(), symbol);
    super.enterTraitDeclaration(ctx);
  }

  @Override
  public void exitTraitDeclaration(EK9Parser.TraitDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkMethodOverrides.accept(symbol);
    super.exitTraitDeclaration(ctx);
  }

  @Override
  public void exitComponentDeclaration(EK9Parser.ComponentDeclarationContext ctx) {
    var symbol = (AggregateSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkDefaultOperators.andThen(checkMethodOverrides).accept(symbol);
    super.exitComponentDeclaration(ctx);
  }

  /**
   * IMPORTANT, because we automatically make supers of functions that meet specific signatures
   * (where they don't have a super) it means that behind the scenes we alter a function to have a
   * parameterised generic super function. But the initial (phase 3) creation does not fully populate
   * all the types. (This is because we want to support inference in other areas).
   * But it also means that the call to expand the arguments and types is done in two stages.
   * Normally the second stage is done in the FULL_RESOLUTION when part of source that defines a parameterised
   * type is processed again. But because these parameterised types are synthetically created it is important to
   * expand them in on function entry. Because on exit there will be argument checks.
   *
   * @param ctx the parse tree
   */
  @Override
  public void enterFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {

    var symbol = (FunctionSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    symbol.getSuperFunction().ifPresent(this::reResolveParameterisedType);

    super.enterFunctionDeclaration(ctx);
  }

  @Override
  public void exitFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    var symbol = (FunctionSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkFunctionOverrides.accept(symbol);
    super.exitFunctionDeclaration(ctx);
  }

  @Override
  public void enterDynamicFunctionDeclaration(EK9Parser.DynamicFunctionDeclarationContext ctx) {
    var symbol = (FunctionSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    symbol.getSuperFunction().ifPresent(this::reResolveParameterisedType);

    //Automatically populate the function signature and return for a dynamic function
    autoMatchSuperFunctionSignature.accept(symbol);
    //Now just check it, should be fine - maybe remove in the future.
    checkFunctionOverrides.accept(symbol);

    super.enterDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void exitDynamicFunctionDeclaration(EK9Parser.DynamicFunctionDeclarationContext ctx) {
    //But check that if the super had no implementation this dynamic function does.
    checkForDynamicFunctionBody.accept(ctx);

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
      processTypeConstraint.accept(aggregateSymbol, ctx.constrainDeclaration());

    }
    super.exitTypeDeclaration(ctx);
  }

  @Override
  public void exitRegisterStatement(EK9Parser.RegisterStatementContext ctx) {
    checkServiceRegistration.accept(ctx);
    super.exitRegisterStatement(ctx);
  }

  private void reResolveParameterisedType(final ISymbol symbol) {
    if (symbol instanceof PossibleGenericSymbol possibleGenericSymbol && possibleGenericSymbol.isParameterisedType()) {
      symbolAndScopeManagement.resolveOrDefine(possibleGenericSymbol, errorListener);
    }
  }
}
