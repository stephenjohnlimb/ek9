package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DUPLICATE_PROPERTY_FIELD;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DYNAMIC_CLASS_MUST_IMPLEMENT_ABSTRACTS;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_MARKED_ABSTRACT_BUT_IS_ABSTRACT;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
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
 * <p>
 * In general the listener methods in this class and it's supers are either very simple processing or just
 * delegate the processing to a Function/Consumer to 'Process' or 'emit' compiler errors.
 * </p>
 */
final class ResolveDefineInferredTypeListener extends ExpressionsListener {
  private final DynamicCaptureOrError dynamicCaptureOrError;
  private final NoDuplicatedPropertyNamesOrError noDuplicatedPropertyNamesOrError;
  private final DefaultOperatorsOrError defaultOperatorsOrError;
  private final ValidDispatcherMethodsOrError validDispatcherMethodsOrError;
  private final MethodOverridesOrError methodOverridesOrError;
  private final MethodOverridesOrError checkDynamicClassMethodOverrides;
  private final CheckConflictingMethods checkNoConflictingMethods;
  private final FunctionOverridesOrError functionOverridesOrError;
  private final AutoMatchSuperFunctionSignature autoMatchSuperFunctionSignature;
  private final DynamicFunctionBodyPresentOrError dynamicFunctionBodyPresentOrError;
  private final AllTextBodiesPresentOrError allTextBodiesPresentOrError;
  private final ServiceRegistrationOrError serviceRegistrationOrError;
  private final TypeConstraintOrError typeConstraintOrError;
  private final AugmentAggregateWithTraitMethods augmentAggregateWithTraitMethods;
  private final ResolveByTraitVariables resolveByTraitVariables;
  private final NoTraitByVariablesOrError noTraitByVariablesOrErrorOrError;
  private final EnumeratedTypeOrError enumeratedTypeOrError;

  /**
   * Create a new instance to define or resolve inferred types.
   */
  ResolveDefineInferredTypeListener(final ParsedModule parsedModule) {
    super(parsedModule);

    this.dynamicCaptureOrError =
        new DynamicCaptureOrError(symbolsAndScopes, errorListener, symbolFactory);
    this.noDuplicatedPropertyNamesOrError =
        new NoDuplicatedPropertyNamesOrError(symbolsAndScopes, errorListener, DUPLICATE_PROPERTY_FIELD);
    this.defaultOperatorsOrError =
        new DefaultOperatorsOrError(symbolsAndScopes, errorListener);
    this.validDispatcherMethodsOrError =
        new ValidDispatcherMethodsOrError(symbolsAndScopes, errorListener);
    this.methodOverridesOrError =
        new MethodOverridesOrError(symbolsAndScopes, errorListener, NOT_MARKED_ABSTRACT_BUT_IS_ABSTRACT);
    this.checkDynamicClassMethodOverrides =
        new MethodOverridesOrError(symbolsAndScopes, errorListener, DYNAMIC_CLASS_MUST_IMPLEMENT_ABSTRACTS);
    this.checkNoConflictingMethods =
        new CheckConflictingMethods(symbolsAndScopes, errorListener);
    this.functionOverridesOrError =
        new FunctionOverridesOrError(symbolsAndScopes, errorListener);
    this.dynamicFunctionBodyPresentOrError =
        new DynamicFunctionBodyPresentOrError(symbolsAndScopes, errorListener);
    this.autoMatchSuperFunctionSignature =
        new AutoMatchSuperFunctionSignature(symbolsAndScopes, errorListener);
    this.allTextBodiesPresentOrError =
        new AllTextBodiesPresentOrError(symbolsAndScopes, errorListener);
    this.serviceRegistrationOrError =
        new ServiceRegistrationOrError(symbolsAndScopes, errorListener);
    this.typeConstraintOrError =
        new TypeConstraintOrError(symbolsAndScopes, symbolFactory, errorListener);
    this.augmentAggregateWithTraitMethods =
        new AugmentAggregateWithTraitMethods(symbolsAndScopes, errorListener);
    this.resolveByTraitVariables =
        new ResolveByTraitVariables(symbolsAndScopes, errorListener);
    this.noTraitByVariablesOrErrorOrError =
        new NoTraitByVariablesOrError(symbolsAndScopes, errorListener);
    this.enumeratedTypeOrError =
        new EnumeratedTypeOrError(symbolsAndScopes, symbolFactory, errorListener);
  }

  @Override
  public void enterParameterisedType(final EK9Parser.ParameterisedTypeContext ctx) {

    //So finally at this point - as a parameterized type is explicitly encountered
    //WE MUST re-resolve it and in this FULL_COMPILATION phase it will then get all it's types
    //substituted - unless they have already been substituted.
    final var theType = symbolsAndScopes.getRecordedSymbol(ctx);
    if (theType instanceof PossibleGenericSymbol possibleGenericSymbol) {
      reResolveParameterisedType(possibleGenericSymbol);
    } else {
      throw new CompilerException("Expecting parameterised type to exist.");
    }

    super.enterParameterisedType(ctx);
  }


  @Override
  public void enterDynamicVariableCapture(final EK9Parser.DynamicVariableCaptureContext ctx) {

    if (symbolsAndScopes.getRecordedScope(ctx) instanceof CaptureScope captureScope) {
      captureScope.setOpenToEnclosingScope(true);
    }
    super.enterDynamicVariableCapture(ctx);

  }

  @Override
  public void exitDynamicVariableCapture(final EK9Parser.DynamicVariableCaptureContext ctx) {

    dynamicCaptureOrError.accept(ctx);
    super.exitDynamicVariableCapture(ctx);

  }

  @Override
  public void enterTypeDeclaration(final EK9Parser.TypeDeclarationContext ctx) {

    enumeratedTypeOrError.accept(ctx);
    super.enterTypeDeclaration(ctx);

  }

  @Override
  public void exitRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof AggregateSymbol aggregate) {
      defaultOperatorsOrError
          .andThen(noDuplicatedPropertyNamesOrError)
          .andThen(methodOverridesOrError)
          .accept(aggregate);
    }
    super.exitRecordDeclaration(ctx);

  }

  @Override
  public void enterClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof AggregateWithTraitsSymbol aggregate) {
      aggregate.getSuperAggregate().ifPresent(this::reResolveParameterisedType);

      //Now we may modify this class definition if it uses 'traits by'
      //We do this in the entry, because on exit (below) we will check all abstract methods implemented.
      if (checkNoConflictingMethods.test(aggregate)) {
        augmentAggregateWithTraitMethods.accept(ctx.traitsList(), aggregate);
      }
    }
    super.enterClassDeclaration(ctx);

  }

  @Override
  public void exitClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    final var symbol = (AggregateWithTraitsSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    defaultOperatorsOrError
        .andThen(methodOverridesOrError)
        .andThen(validDispatcherMethodsOrError)
        .accept(symbol);
    resolveByTraitVariables.accept(ctx.traitsList(), symbol);

    super.exitClassDeclaration(ctx);
  }

  @Override
  public void enterDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    final var symbol = (AggregateWithTraitsSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    symbol.getSuperAggregate().ifPresent(this::reResolveParameterisedType);

    //Now we may modify this class definition if it uses 'traits by'
    //We do this in the entry, because on exit (below) we will check all abstract methods implemented.
    if (checkNoConflictingMethods.test(symbol)) {
      augmentAggregateWithTraitMethods.accept(ctx.traitsList(), symbol);
    }

    super.enterDynamicClassDeclaration(ctx);
  }

  @Override
  public void exitDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    final var symbol = (AggregateWithTraitsSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    defaultOperatorsOrError
        .andThen(checkDynamicClassMethodOverrides)
        .accept(symbol);
    resolveByTraitVariables.accept(ctx.traitsList(), symbol);

    super.exitDynamicClassDeclaration(ctx);
  }

  @Override
  public void enterTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    final var symbol = (AggregateWithTraitsSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    checkNoConflictingMethods.test(symbol);
    noTraitByVariablesOrErrorOrError.accept(ctx.traitsList(), symbol);

    super.enterTraitDeclaration(ctx);
  }

  @Override
  public void exitTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    final var symbol = (AggregateWithTraitsSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    methodOverridesOrError.accept(symbol);

    super.exitTraitDeclaration(ctx);
  }

  @Override
  public void exitComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {

    final var symbol = (AggregateSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    defaultOperatorsOrError.andThen(methodOverridesOrError).accept(symbol);

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
  public void enterFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    final var symbol = (FunctionSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    symbol.getSuperFunction().ifPresent(this::reResolveParameterisedType);

    super.enterFunctionDeclaration(ctx);
  }

  @Override
  public void exitFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    final var symbol = (FunctionSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    functionOverridesOrError.accept(symbol);

    super.exitFunctionDeclaration(ctx);
  }

  @Override
  public void enterDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    final var symbol = (FunctionSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    symbol.getSuperFunction().ifPresent(this::reResolveParameterisedType);

    //Automatically populate the function signature and return for a dynamic function
    autoMatchSuperFunctionSignature.accept(symbol);
    //Now just check it, should be fine - maybe remove in the future.
    functionOverridesOrError.accept(symbol);

    super.enterDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void exitDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    //But check that if the super had no implementation this dynamic function does.
    dynamicFunctionBodyPresentOrError.accept(ctx);

    super.exitDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void exitTextDeclaration(final EK9Parser.TextDeclarationContext ctx) {

    //At this point, all the text declarations and their text bodies will have been defined.
    //But also the 'super' with all the methods will have also been defined.
    //This is the point where we can check that this specific text declaration has all the same methods as the super.
    final var textAggregate = (AggregateSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    if (textAggregate != null) {
      //Only check if valid - might have been a duplicate.
      allTextBodiesPresentOrError.accept(textAggregate);
    }

    super.exitTextDeclaration(ctx);
  }

  @Override
  public void exitTypeDeclaration(final EK9Parser.TypeDeclarationContext ctx) {

    final var aggregateSymbol = (AggregateSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    if (aggregateSymbol != null && ctx.constrainDeclaration() != null) {
      //Then it is constrained type, so we need to check that the appropriate operators
      //used in these constraints actually exist.
      typeConstraintOrError.accept(aggregateSymbol, ctx.constrainDeclaration());
    }

    super.exitTypeDeclaration(ctx);
  }

  @Override
  public void exitRegisterStatement(final EK9Parser.RegisterStatementContext ctx) {

    serviceRegistrationOrError.accept(ctx);

    super.exitRegisterStatement(ctx);
  }

  private void reResolveParameterisedType(final ISymbol symbol) {

    if (symbol instanceof PossibleGenericSymbol possibleGenericSymbol && possibleGenericSymbol.isParameterisedType()) {
      symbolsAndScopes.resolveOrDefine(possibleGenericSymbol, errorListener);
    }

  }
}
