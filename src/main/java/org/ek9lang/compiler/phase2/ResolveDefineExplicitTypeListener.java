package org.ek9lang.compiler.phase2;

import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.antlr.EK9BaseListener;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.common.ScopeStack;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.AccessGenericInGeneric;
import org.ek9lang.compiler.support.AggregateFactory;
import org.ek9lang.compiler.support.MostSpecificScope;
import org.ek9lang.compiler.support.NoDuplicateOperationsOrError;
import org.ek9lang.compiler.support.NoNameCollisionOrError;
import org.ek9lang.compiler.support.ResolveOrDefineExplicitParameterizedType;
import org.ek9lang.compiler.support.ResolveOrDefineIdentifierReference;
import org.ek9lang.compiler.support.ResolveOrDefineTypeDef;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.CaptureScope;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.ServiceOperationSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * <p>
 * A bit of a long-winded name, but this is really the second pass of the second phase of compilation.
 * The first pass will have defined lots of types, but in the case of explicit (non-inferred uses) of
 * template/generic types - definition will not have been possible during the first pass.
 * </p>
 * <p>
 * At the end of this source file there are basic lookups being done to record symbols against contexts.
 * Also, the association to types being extended could not be done in the very first pass.
 * So this pass also hooks up the super types/function - by resolving them.
 * </p>
 * <p>
 * It is important to do this 'supers' bit now - because the generic types can be referenced in bodies.
 * So as they are explicitly used in terms of 'T', 'K' and 'V' etc. in subtypes/functions we need them
 * to be resolvable via the type/function hierarchy.
 * </p>
 * <p>
 * Note, we're not trying to resolve normal variables and parameters in this phase, but parametric types.
 * There's a reason everyone leaves out Generics/Templates - it's really hard.
 * It is now a hard fail if explicit type cannot be resolved. Not inferred types - not yet.
 * This is due to definition ordering and also the fact that each file is processed concurrently.
 * </p>
 * <p>
 * So, ordering is not guaranteed, the first pass - just accepts this and resolves/defines what it can.
 * But this second pass in the first phase will need to raise errors if it cannot resolve or define uses
 * of types/polymorphic parameterization - when it is declared and explicit (not inferred).
 * Phase 1 Definition first pass will have defined this or failed, and we won't even get this running.
 * </p>
 * <p>
 * In addition this phase also attempts to check/resolve any aggregate properties, in some cases these can be inferred
 * sorts of declarations and it is possible to work them out. But this is only for simple inferences.
 * See exitVariableDeclaration and how this uses a consumer to check this and either resolve types or raise errors.
 * </p>
 */
final class ResolveDefineExplicitTypeListener extends EK9BaseListener {
  private final SymbolsAndScopes symbolsAndScopes;
  private final ResolveOrDefineIdentifierReference resolveOrDefineIdentifierReference;
  private final ResolveOrDefineTypeDef resolveOrDefineTypeDef;
  private final ResolveOrDefineExplicitParameterizedType resolveOrDefineExplicitParameterizedType;
  private final ValidOperatorOrError validOperatorOrError;
  private final SetupGenericTOrError setupGenericTOrError;
  private final ValidServiceOperationOrError validServiceOperationOrError;
  private final NoDuplicatedServicePathsOrError noDuplicatedServicePathsOrError;
  private final VisibilityOfOperationsOrError visibilityOfOperationsOrError;
  private final NoDuplicateOperationsOrError noDuplicateOperationsOrError;
  private final SuitableToExtendOrError recordSuitableToExtendOrError;
  private final SuitableToExtendOrError classTraitSuitableToExtendOrError;
  private final SuitableToExtendOrError classSuitableToExtendOrError;
  private final SuitableToExtendOrError componentSuitableToExtendOrError;
  private final SuitableGenusOrError registerGenusOrError;
  private final SuitableGenusOrError applicationForProgramOrError;
  private final SyntheticConstructorCreator syntheticConstructorCreator;
  private final MostSpecificScope mostSpecificScope;
  private final NoNameCollisionOrError noNameCollisionOrError;
  private final AccessGenericInGeneric accessGenericInGeneric;
  private final ProcessTypeDeclarationOrError processTypeDeclarationOrError;
  private final ProcessFunctionDeclarationOrError processFunctionDeclarationOrError;
  private final ProcessTraitDeclarationOrError processTraitDeclarationOrError;
  private final ProcessVariableOnlyOrError processVariableOnlyOrError;
  private final ProcessVariableOrError processVariableOrError;
  private final ProcessDynamicClassOrError processDynamicClassOrError;
  private final ProcessDynamicFunctionOrError processDynamicFunctionOrError;

  /**
   * Still defining some stuff here, but also resolving where possible.
   * This second pass to try and resolve types due to declaration ordering.
   */
  ResolveDefineExplicitTypeListener(final ParsedModule parsedModule) {

    final var aggregateFactory = new AggregateFactory(parsedModule.getEk9Types());
    final var errorListener = parsedModule.getSource().getErrorListener();
    final var symbolFactory = new SymbolFactory(parsedModule);

    //At construction the ScopeStack will push the module scope on to the stack.
    this.symbolsAndScopes =
        new SymbolsAndScopes(parsedModule, new ScopeStack(parsedModule.getModuleScope()));
    this.syntheticConstructorCreator =
        new SyntheticConstructorCreator(aggregateFactory);
    this.noDuplicateOperationsOrError =
        new NoDuplicateOperationsOrError(errorListener);
    this.visibilityOfOperationsOrError =
        new VisibilityOfOperationsOrError(symbolsAndScopes, errorListener);
    this.resolveOrDefineIdentifierReference =
        new ResolveOrDefineIdentifierReference(symbolsAndScopes, symbolFactory, errorListener, false);
    this.resolveOrDefineTypeDef =
        new ResolveOrDefineTypeDef(symbolsAndScopes, symbolFactory, errorListener, true);
    this.validOperatorOrError =
        new ValidOperatorOrError(symbolsAndScopes, errorListener);
    this.setupGenericTOrError =
        new SetupGenericTOrError(symbolsAndScopes, aggregateFactory, errorListener);
    this.validServiceOperationOrError =
        new ValidServiceOperationOrError(symbolsAndScopes, errorListener);
    this.noDuplicatedServicePathsOrError =
        new NoDuplicatedServicePathsOrError(symbolsAndScopes, errorListener);
    this.resolveOrDefineExplicitParameterizedType =
        new ResolveOrDefineExplicitParameterizedType(symbolsAndScopes, symbolFactory, errorListener, true);
    this.recordSuitableToExtendOrError =
        new SuitableToExtendOrError(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.RECORD, false);
    this.classTraitSuitableToExtendOrError =
        new SuitableToExtendOrError(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.CLASS_TRAIT, true);
    this.classSuitableToExtendOrError =
        new SuitableToExtendOrError(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.CLASS, true);
    this.componentSuitableToExtendOrError =
        new SuitableToExtendOrError(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.COMPONENT, true);
    this.registerGenusOrError =
        new SuitableGenusOrError(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.COMPONENT, false, true);
    this.applicationForProgramOrError =
        new SuitableGenusOrError(symbolsAndScopes, errorListener,
            List.of(ISymbol.SymbolGenus.GENERAL_APPLICATION, ISymbol.SymbolGenus.SERVICE_APPLICATION), false, true);
    this.accessGenericInGeneric =
        new AccessGenericInGeneric(symbolsAndScopes);
    this.mostSpecificScope =
        new MostSpecificScope(symbolsAndScopes);
    this.noNameCollisionOrError =
        new NoNameCollisionOrError(errorListener, false);
    this.processTypeDeclarationOrError =
        new ProcessTypeDeclarationOrError(symbolsAndScopes, aggregateFactory, errorListener);
    this.processFunctionDeclarationOrError =
        new ProcessFunctionDeclarationOrError(symbolsAndScopes, symbolFactory, errorListener);
    this.processTraitDeclarationOrError =
        new ProcessTraitDeclarationOrError(symbolsAndScopes, errorListener);
    this.processVariableOnlyOrError =
        new ProcessVariableOnlyOrError(symbolsAndScopes, errorListener);
    this.processVariableOrError =
        new ProcessVariableOrError(symbolsAndScopes, symbolFactory, errorListener);
    this.processDynamicClassOrError =
        new ProcessDynamicClassOrError(symbolsAndScopes, errorListener);
    this.processDynamicFunctionOrError =
        new ProcessDynamicFunctionOrError(symbolsAndScopes, errorListener);
  }

  @Override
  public void exitTypeDeclaration(final EK9Parser.TypeDeclarationContext ctx) {

    processTypeDeclarationOrError.accept(ctx);
    super.exitTypeDeclaration(ctx);

  }

  /**
   * Process a function declaration, so this is at the very top level of construct definition.
   * This is NOT a dynamic function.
   */
  @Override
  public void enterFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterFunctionDeclaration(ctx);

  }

  @Override
  public void exitFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    processFunctionDeclarationOrError.accept(ctx);
    symbolsAndScopes.exitScope();
    super.exitFunctionDeclaration(ctx);

  }

  @Override
  public void enterRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterRecordDeclaration(ctx);

  }

  @Override
  public void exitRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof AggregateSymbol asAggregate) {
      processExtendableConstruct(new Ek9Token(ctx.start), asAggregate, ctx.extendDeclaration(),
          recordSuitableToExtendOrError);
    }
    symbolsAndScopes.exitScope();
    super.exitRecordDeclaration(ctx);

  }

  @Override
  public void enterTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterTraitDeclaration(ctx);

  }

  @Override
  public void exitTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    processTraitDeclarationOrError.accept(ctx);
    symbolsAndScopes.exitScope();
    super.exitTraitDeclaration(ctx);

  }

  @Override
  public void enterClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterClassDeclaration(ctx);

  }

  @Override
  public void exitClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof AggregateWithTraitsSymbol asAggregate) {
      processExtendableConstruct(new Ek9Token(ctx.start), asAggregate, ctx.extendDeclaration(),
          classSuitableToExtendOrError);

      if (ctx.traitsList() != null) {
        ctx.traitsList().traitReference().forEach(traitRef -> {
          final var resolved = classTraitSuitableToExtendOrError.apply(traitRef.identifierReference());
          resolved.ifPresent(theTrait -> asAggregate.addTrait((AggregateWithTraitsSymbol) theTrait));
        });
      }
    }
    symbolsAndScopes.exitScope();
    super.exitClassDeclaration(ctx);

  }



  @Override
  public void enterComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterComponentDeclaration(ctx);

  }

  @Override
  public void exitComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof AggregateSymbol asAggregate) {
      processExtendableConstruct(new Ek9Token(ctx.start), asAggregate, ctx.extendDeclaration(),
          componentSuitableToExtendOrError);
    }
    symbolsAndScopes.exitScope();
    super.exitComponentDeclaration(ctx);

  }

  @Override
  public void enterTextDeclaration(final EK9Parser.TextDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterTextDeclaration(ctx);

  }

  @Override
  public void exitTextDeclaration(final EK9Parser.TextDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitTextDeclaration(ctx);

  }

  @Override
  public void enterTextBodyDeclaration(final EK9Parser.TextBodyDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterTextBodyDeclaration(ctx);

  }

  @Override
  public void exitTextBodyDeclaration(final EK9Parser.TextBodyDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitTextBodyDeclaration(ctx);

  }

  @Override
  public void enterServiceDeclaration(final EK9Parser.ServiceDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterServiceDeclaration(ctx);

  }

  @Override
  public void exitServiceDeclaration(final EK9Parser.ServiceDeclarationContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof AggregateSymbol asAggregate) {
      //Check the set of checks to run on the aggregate.
      final var serviceChecks = syntheticConstructorCreator
          .andThen(visibilityOfOperationsOrError)
          .andThen(noDuplicatedServicePathsOrError);

      noDuplicateOperationsOrError.accept(new Ek9Token(ctx.start), asAggregate);
      serviceChecks.accept(asAggregate);
    }
    symbolsAndScopes.exitScope();
    super.exitServiceDeclaration(ctx);

  }

  @Override
  public void enterServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterServiceOperationDeclaration(ctx);

  }

  @Override
  public void exitServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    if (symbolsAndScopes.getRecordedScope(ctx) instanceof ServiceOperationSymbol serviceOperation) {
      validServiceOperationOrError.accept(serviceOperation, ctx);
    }
    symbolsAndScopes.exitScope();
    super.exitServiceOperationDeclaration(ctx);

  }

  @Override
  public void enterApplicationDeclaration(final EK9Parser.ApplicationDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterApplicationDeclaration(ctx);

  }

  @Override
  public void exitApplicationDeclaration(final EK9Parser.ApplicationDeclarationContext ctx) {

    symbolsAndScopes.exitScope();
    super.exitApplicationDeclaration(ctx);

  }

  @Override
  public void enterDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterDynamicClassDeclaration(ctx);

  }

  /**
   * Dynamic classes can 'extend' a parameterised generic type.
   * But they can also (separately) implement one or more Traits.
   * They cannot extend a normal class, this is to promote composition.
   */
  @Override
  public void exitDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    processDynamicClassOrError.accept(ctx);
    symbolsAndScopes.exitScope();
    super.exitDynamicClassDeclaration(ctx);

  }

  @Override
  public void enterDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterDynamicFunctionDeclaration(ctx);

  }

  /**
   * Dynamic functions can extend open/abstract normal functions, they can also (separately)
   * extends parameterised generic functions.
   */
  @Override
  public void exitDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    processDynamicFunctionOrError.accept(ctx);
    symbolsAndScopes.exitScope();
    super.exitDynamicFunctionDeclaration(ctx);

  }

  @Override
  public void enterDynamicVariableCapture(final EK9Parser.DynamicVariableCaptureContext ctx) {

    final CaptureScope scope = (CaptureScope) symbolsAndScopes.enterScope(ctx);
    scope.setOpenToEnclosingScope(true);
    super.enterDynamicVariableCapture(ctx);

  }

  @Override
  public void exitDynamicVariableCapture(final EK9Parser.DynamicVariableCaptureContext ctx) {

    final CaptureScope scope = (CaptureScope) symbolsAndScopes.getRecordedScope(ctx);
    scope.setOpenToEnclosingScope(false);
    symbolsAndScopes.exitScope();
    super.exitDynamicVariableCapture(ctx);

  }

  @Override
  public void enterMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);

    //This can be other types i.e. aggregate in case of Programs for example.
    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof MethodSymbol methodSymbol && !methodSymbol.isConstructor()) {
      //Now this was not called in the first phase, and we only call it for non-constructors
      //(because they must have same name as a type)
      noNameCollisionOrError.test(mostSpecificScope.get(), methodSymbol);
    }
    super.enterMethodDeclaration(ctx);

  }

  @Override
  public void exitMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    if (ctx.identifierReference() != null) {
      applicationForProgramOrError.apply(ctx.identifierReference());
    }

    symbolsAndScopes.exitScope();
    super.exitMethodDeclaration(ctx);

  }

  @Override
  public void enterOperatorDeclaration(final EK9Parser.OperatorDeclarationContext ctx) {

    symbolsAndScopes.enterScope(ctx);
    super.enterOperatorDeclaration(ctx);

  }

  @Override
  public void exitOperatorDeclaration(final EK9Parser.OperatorDeclarationContext ctx) {

    if (symbolsAndScopes.getTopScope() instanceof MethodSymbol method) {
      //Yes this is correct an operator is just a method but marked as an operator.
      validOperatorOrError.accept(method, ctx);
    }
    symbolsAndScopes.exitScope();
    super.exitOperatorDeclaration(ctx);

  }

  /**
   * It's important to reprocess this now, just to ensure that any returning types are defined on methods.
   * This is because it might not have been possible on the first pass - depending on ordering.
   * It might still not be set of the type cannot be resolved, but that's OK, other errors will pick that up.
   */
  @Override
  public void exitReturningParam(final EK9Parser.ReturningParamContext ctx) {

    //Now get back to the parent scope, function, method, try, switch etc.
    super.exitReturningParam(ctx);

    final ParseTree child =
        ctx.variableDeclaration() != null ? ctx.variableDeclaration() : ctx.variableOnlyDeclaration();
    final var symbol = symbolsAndScopes.getRecordedSymbol(child);
    if (symbolsAndScopes.getTopScope() instanceof MethodSymbol methodSymbol) {
      methodSymbol.setType(symbol.getType());
    }

  }

  @Override
  public void exitVariableOnlyDeclaration(final EK9Parser.VariableOnlyDeclarationContext ctx) {

    processVariableOnlyOrError.accept(ctx);
    super.exitVariableOnlyDeclaration(ctx);

  }

  @Override
  public void exitVariableDeclaration(final EK9Parser.VariableDeclarationContext ctx) {

    processVariableOrError.accept(ctx);
    super.exitVariableDeclaration(ctx);

  }

  @Override
  public void exitRegisterStatement(final EK9Parser.RegisterStatementContext ctx) {

    //If there are any services registered then the application genus is modified from
    //being a GENERAL_APPLICATION to a SERVICE_APPLICATION.
    if (ctx.identifierReference() != null) {
      //Then it's component registration; so check it.
      registerGenusOrError.apply(ctx.identifierReference());
    }

    //Else it is probably a service which is dealt with in the next phase.
    super.exitRegisterStatement(ctx);

  }

  /**
   * Now at this point even parametric polymorphic types should be resolved.
   * Not by inference, but in explicit form. Which is all we need to populate the 'T'
   * in the case of them being constrained.
   */
  @Override
  public void exitParameterisedDetail(final EK9Parser.ParameterisedDetailContext ctx) {

    setupGenericTOrError.accept(ctx);
    super.exitParameterisedDetail(ctx);

  }

  @Override
  public void exitTypeDef(final EK9Parser.TypeDefContext ctx) {

    accessGenericInGeneric.apply(symbolsAndScopes.getRecordedSymbol(ctx))
        .ifPresent(genericData -> genericData.parent().addGenericSymbolReference(genericData.dependent()));
    super.exitTypeDef(ctx);

  }

  /*
    -------------- Lookup and record items --------------------
    The lookup process here, will automatically issue semantic errors if there is no
    resolution. This will leave the ctx without any recorded symbol.
    This is important for other parts of ths phase as that means they must be aware
    that the ctx may not have a scope or symbol recorded against them.
  */

  @Override
  public void enterIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {

    //Now this is a bit different as we need to resolve in the current scope.
    resolveOrDefineIdentifierReference.apply(ctx).ifPresent(symbol -> symbolsAndScopes.recordSymbol(symbol, ctx));
    super.enterIdentifierReference(ctx);

  }

  @Override
  public void enterTypeDef(final EK9Parser.TypeDefContext ctx) {

    resolveOrDefineTypeDef.apply(ctx).ifPresent(symbol -> symbolsAndScopes.recordSymbol(symbol, ctx));
    super.enterTypeDef(ctx);

  }

  @Override
  public void enterParameterisedType(final EK9Parser.ParameterisedTypeContext ctx) {

    resolveOrDefineExplicitParameterizedType.apply(ctx).ifPresent(symbol -> symbolsAndScopes.recordSymbol(symbol, ctx));
    super.enterParameterisedType(ctx);

  }

  private void processExtendableConstruct(final IToken token, final AggregateSymbol symbol,
                                          final EK9Parser.ExtendDeclarationContext extendDeclaration,
                                          final SuitableToExtendOrError extendChecker) {

    syntheticConstructorCreator.andThen(visibilityOfOperationsOrError).accept(symbol);
    noDuplicateOperationsOrError.accept(token, symbol);

    if (extendDeclaration != null) {
      extendChecker.apply(extendDeclaration.typeDef())
          .ifPresent(theSuper -> symbol.setSuperAggregate((IAggregateSymbol) theSuper));
    } else {
      symbol.getType().ifPresent(theType -> {
        if (ISymbol.SymbolGenus.CLASS.equals(theType.getGenus())
            && !theType.isGenericInNature()
            && !symbolsAndScopes.getEk9Types().ek9AnyClass().equals(theType)) {
          symbol.setSuperAggregate((IAggregateSymbol) symbolsAndScopes.getEk9Types().ek9AnyClass());
        }
      });
    }

  }

}


