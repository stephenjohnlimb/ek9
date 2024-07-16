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
import org.ek9lang.compiler.support.CheckForDuplicateOperations;
import org.ek9lang.compiler.support.MostSpecificScope;
import org.ek9lang.compiler.support.NameCollisionChecker;
import org.ek9lang.compiler.support.ResolveOrDefineExplicitParameterizedType;
import org.ek9lang.compiler.support.ResolveOrDefineIdentifierReference;
import org.ek9lang.compiler.support.ResolveOrDefineTypeDef;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.CaptureScope;
import org.ek9lang.compiler.symbols.Ek9Types;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.ServiceOperationSymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

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
public final class ResolveDefineExplicitTypeListener extends EK9BaseListener {
  private final SymbolsAndScopes symbolsAndScopes;
  private final ResolveOrDefineIdentifierReference resolveOrDefineIdentifierReference;
  private final ResolveOrDefineTypeDef resolveOrDefineTypeDef;
  private final SynthesizeSuperFunction synthesizeSuperFunction;
  private final ResolveOrDefineExplicitParameterizedType resolveOrDefineExplicitParameterizedType;
  private final CheckOperator checkOperator;
  private final SetupGenericT setupGenericT;
  private final CheckServiceOperation checkServiceOperation;
  private final CheckDuplicatedServicePaths checkDuplicatedServicePaths;
  private final CheckVisibilityOfOperations checkVisibilityOfOperations;
  private final CheckForDuplicateOperations checkForDuplicateOperations;
  private final CheckNotGenericTypeParameter checkNotGenericTypeParameter;
  private final CheckSuitableToExtend checkFunctionSuitableToExtend;
  private final CheckSuitableToExtend checkRecordSuitableToExtend;
  private final CheckSuitableToExtend checkClassTraitSuitableToExtend;
  private final CheckSuitableToExtend checkClassSuitableToExtend;
  private final CheckSuitableToExtend checkComponentSuitableToExtend;
  private final CheckSuitableGenus checkRegisterGenusValid;
  private final CheckSuitableGenus checkAllowedClassSuitableGenus;
  private final CheckSuitableGenus checkApplicationForProgram;
  private final SyntheticConstructorCreator syntheticConstructorCreator;
  private final CheckAndPopulateConstrainedType checkAndPopulateConstrainedType;
  private final MostSpecificScope mostSpecificScope;
  private final NameCollisionChecker nameCollisionChecker;
  private final AccessGenericInGeneric accessGenericInGeneric;
  private final AggregateFactory aggregateFactory;
  private final Ek9Types ek9Types;
  private final ProcessContextVariableDeclaration processContextVariableDeclaration;


  /**
   * Still defining some stuff here, but also resolving where possible.
   * This second pass to try and resolve types due to declaration ordering.
   */
  ResolveDefineExplicitTypeListener(final ParsedModule parsedModule) {

    //At construction the ScopeStack will push the module scope on to the stack.
    this.symbolsAndScopes = new SymbolsAndScopes(parsedModule,
        new ScopeStack(parsedModule.getModuleScope()));
    this.ek9Types = parsedModule.getEk9Types();
    this.aggregateFactory = new AggregateFactory(ek9Types);
    this.syntheticConstructorCreator = new SyntheticConstructorCreator(aggregateFactory);

    final var errorListener =
        parsedModule.getSource().getErrorListener();
    final SymbolFactory symbolFactory =
        new SymbolFactory(parsedModule);

    this.checkForDuplicateOperations =
        new CheckForDuplicateOperations(errorListener);
    this.checkVisibilityOfOperations =
        new CheckVisibilityOfOperations(symbolsAndScopes, errorListener);
    this.checkNotGenericTypeParameter =
        new CheckNotGenericTypeParameter(errorListener);
    this.resolveOrDefineIdentifierReference =
        new ResolveOrDefineIdentifierReference(symbolsAndScopes, symbolFactory, errorListener, false);
    this.resolveOrDefineTypeDef =
        new ResolveOrDefineTypeDef(symbolsAndScopes, symbolFactory, errorListener, true);
    this.synthesizeSuperFunction =
        new SynthesizeSuperFunction(symbolsAndScopes, symbolFactory, errorListener);
    this.checkOperator =
        new CheckOperator(symbolsAndScopes, errorListener);
    this.setupGenericT =
        new SetupGenericT(symbolsAndScopes, aggregateFactory, errorListener);
    this.checkServiceOperation =
        new CheckServiceOperation(symbolsAndScopes, errorListener);
    this.checkDuplicatedServicePaths =
        new CheckDuplicatedServicePaths(symbolsAndScopes, errorListener);
    this.resolveOrDefineExplicitParameterizedType =
        new ResolveOrDefineExplicitParameterizedType(symbolsAndScopes, symbolFactory, errorListener, true);
    this.checkFunctionSuitableToExtend =
        new CheckSuitableToExtend(symbolsAndScopes, errorListener,
            List.of(ISymbol.SymbolGenus.FUNCTION, ISymbol.SymbolGenus.FUNCTION_TRAIT), true);
    this.checkRecordSuitableToExtend =
        new CheckSuitableToExtend(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.RECORD, false);
    this.checkClassTraitSuitableToExtend =
        new CheckSuitableToExtend(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.CLASS_TRAIT, true);
    this.checkClassSuitableToExtend =
        new CheckSuitableToExtend(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.CLASS, true);
    this.checkComponentSuitableToExtend =
        new CheckSuitableToExtend(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.COMPONENT, true);
    this.checkRegisterGenusValid =
        new CheckSuitableGenus(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.COMPONENT, false, true);
    this.checkAllowedClassSuitableGenus =
        new CheckSuitableGenus(symbolsAndScopes, errorListener, ISymbol.SymbolGenus.CLASS, false, true);
    this.checkApplicationForProgram =
        new CheckSuitableGenus(symbolsAndScopes, errorListener,
            List.of(ISymbol.SymbolGenus.GENERAL_APPLICATION, ISymbol.SymbolGenus.SERVICE_APPLICATION), false, true);
    this.checkAndPopulateConstrainedType =
        new CheckAndPopulateConstrainedType(symbolsAndScopes, aggregateFactory, errorListener);
    this.accessGenericInGeneric =
        new AccessGenericInGeneric(symbolsAndScopes);
    this.mostSpecificScope =
        new MostSpecificScope(symbolsAndScopes);
    this.nameCollisionChecker =
        new NameCollisionChecker(errorListener, false);
    this.processContextVariableDeclaration =
        new ProcessContextVariableDeclaration(symbolsAndScopes, symbolFactory, errorListener);

  }

  @Override
  public void exitTypeDeclaration(final EK9Parser.TypeDeclarationContext ctx) {

    final var aggregateSymbol = (AggregateSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    //Might be null if name if the name is duplicated.
    if (aggregateSymbol != null) {
      aggregateFactory.addSyntheticConstructorIfRequired(aggregateSymbol);
      aggregateFactory.addConstructor(aggregateSymbol, new VariableSymbol("arg", aggregateSymbol));
      if (ctx.typeDef() == null) {
        //For an enumeration we allow creation via String.
        final var constructor =
            aggregateFactory.addConstructor(aggregateSymbol, new VariableSymbol("arg", ek9Types.ek9String()));
        constructor.setMarkedPure(true);
      } else {
        final var theConstrainedType = symbolsAndScopes.getRecordedSymbol(ctx.typeDef());
        checkAndPopulateConstrainedType.accept(aggregateSymbol, theConstrainedType);
        //else we should already get an error for this missing type.
      }
    }

    super.exitTypeDeclaration(ctx);
  }

  /**
   * Process a function declaration, so this is at the very top level of construct definition.
   * This is NOT a dynamic function.
   */
  @Override
  public void enterFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Function should have been defined", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterFunctionDeclaration(ctx);
  }

  @Override
  public void exitFunctionDeclaration(final EK9Parser.FunctionDeclarationContext ctx) {

    final var symbol = (FunctionSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Function should have been defined as symbol", symbol);

    //Normal functions can extend other normal functions if open/abstract - this code below checks.
    if (ctx.identifierReference() != null) {
      final var resolved = checkFunctionSuitableToExtend.apply(ctx.identifierReference());
      resolved.ifPresent(theSuper -> symbol.setSuperFunction((FunctionSymbol) theSuper));
    } else if (!symbol.isGenericInNature()) {
      synthesizeSuperFunction.accept(symbol);
    }
    symbolsAndScopes.exitScope();

    super.exitFunctionDeclaration(ctx);
  }

  @Override
  public void enterRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Record should have been defined as scope", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterRecordDeclaration(ctx);
  }

  @Override
  public void exitRecordDeclaration(final EK9Parser.RecordDeclarationContext ctx) {

    final var symbol = (AggregateSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Record should have been defined as symbol", symbol);
    processExtendableConstruct(new Ek9Token(ctx.start), symbol, ctx.extendDeclaration(), checkRecordSuitableToExtend);
    symbolsAndScopes.exitScope();

    super.exitRecordDeclaration(ctx);
  }

  @Override
  public void enterTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Trait should have been defined", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterTraitDeclaration(ctx);
  }

  @Override
  public void exitTraitDeclaration(final EK9Parser.TraitDeclarationContext ctx) {

    final var symbol = (AggregateWithTraitsSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Trait should have been defined as symbol", symbol);
    checkVisibilityOfOperations.accept(symbol);
    checkForDuplicateOperations.accept(new Ek9Token(ctx.start), symbol);

    if (ctx.traitsList() != null) {
      ctx.traitsList().traitReference().forEach(traitRef -> {
        final var resolved = checkClassTraitSuitableToExtend.apply(traitRef.identifierReference());
        resolved.ifPresent(theTrait -> symbol.addTrait((AggregateWithTraitsSymbol) theTrait));
      });
    }

    if (ctx.allowingOnly() != null) {
      ctx.allowingOnly().identifierReference().forEach(classRef -> {
        final var resolved = checkAllowedClassSuitableGenus.apply(classRef);
        resolved.ifPresent(theClass -> symbol.addAllowedExtender((IAggregateSymbol) theClass));
      });
    }

    symbolsAndScopes.exitScope();

    super.exitTraitDeclaration(ctx);
  }

  @Override
  public void enterClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Class should have been defined", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterClassDeclaration(ctx);
  }

  @Override
  public void exitClassDeclaration(final EK9Parser.ClassDeclarationContext ctx) {

    final var symbol = (AggregateWithTraitsSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Class should have been defined as symbol", symbol);
    processExtendableConstruct(new Ek9Token(ctx.start), symbol, ctx.extendDeclaration(), checkClassSuitableToExtend);

    if (ctx.traitsList() != null) {
      ctx.traitsList().traitReference().forEach(traitRef -> {
        final var resolved = checkClassTraitSuitableToExtend.apply(traitRef.identifierReference());
        resolved.ifPresent(theTrait -> symbol.addTrait((AggregateWithTraitsSymbol) theTrait));
      });
    }

    symbolsAndScopes.exitScope();

    super.exitClassDeclaration(ctx);
  }

  @Override
  public void enterComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Component should have been defined", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterComponentDeclaration(ctx);
  }

  @Override
  public void exitComponentDeclaration(final EK9Parser.ComponentDeclarationContext ctx) {

    final var symbol = (AggregateSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Component should have been defined as symbol", symbol);
    processExtendableConstruct(new Ek9Token(ctx.start), symbol, ctx.extendDeclaration(),
        checkComponentSuitableToExtend);
    symbolsAndScopes.exitScope();

    super.exitComponentDeclaration(ctx);
  }

  @Override
  public void enterTextDeclaration(final EK9Parser.TextDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Text should have been defined", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterTextDeclaration(ctx);
  }

  @Override
  public void exitTextDeclaration(final EK9Parser.TextDeclarationContext ctx) {

    symbolsAndScopes.exitScope();

    super.exitTextDeclaration(ctx);
  }

  @Override
  public void enterTextBodyDeclaration(final EK9Parser.TextBodyDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Text Body should have been defined", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterTextBodyDeclaration(ctx);
  }

  @Override
  public void exitTextBodyDeclaration(final EK9Parser.TextBodyDeclarationContext ctx) {

    symbolsAndScopes.exitScope();

    super.exitTextBodyDeclaration(ctx);
  }

  @Override
  public void enterServiceDeclaration(final EK9Parser.ServiceDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Service should have been defined", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterServiceDeclaration(ctx);
  }

  @Override
  public void exitServiceDeclaration(final EK9Parser.ServiceDeclarationContext ctx) {

    final var symbol = (AggregateSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    syntheticConstructorCreator.accept(symbol);
    checkVisibilityOfOperations.accept(symbol);
    checkForDuplicateOperations.accept(new Ek9Token(ctx.start), symbol);
    checkDuplicatedServicePaths.accept(symbol);

    symbolsAndScopes.exitScope();

    super.exitServiceDeclaration(ctx);
  }

  @Override
  public void enterServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Service Operation should have been defined", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterServiceOperationDeclaration(ctx);
  }

  @Override
  public void exitServiceOperationDeclaration(final EK9Parser.ServiceOperationDeclarationContext ctx) {

    final var currentScope = symbolsAndScopes.getRecordedScope(ctx);
    if (currentScope instanceof ServiceOperationSymbol serviceOperation) {
      checkServiceOperation.accept(serviceOperation, ctx);
    }
    symbolsAndScopes.exitScope();

    super.exitServiceOperationDeclaration(ctx);
  }

  @Override
  public void enterApplicationDeclaration(final EK9Parser.ApplicationDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Application should have been defined", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterApplicationDeclaration(ctx);
  }

  @Override
  public void exitApplicationDeclaration(final EK9Parser.ApplicationDeclarationContext ctx) {

    symbolsAndScopes.exitScope();

    super.exitApplicationDeclaration(ctx);
  }

  @Override
  public void enterDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Dynamic Class should have been defined", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterDynamicClassDeclaration(ctx);
  }

  /**
   * Dynamic classes can 'extend' a parameterised generic type.
   * But they can also (separately) implement one or more Traits.
   * They cannot extend a normal class, this is to promote composition.
   */
  @Override
  public void exitDynamicClassDeclaration(final EK9Parser.DynamicClassDeclarationContext ctx) {

    final var symbol = (AggregateWithTraitsSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Dynamic Class should have been defined as symbol", symbol);

    checkVisibilityOfOperations.accept(symbol);
    checkForDuplicateOperations.accept(new Ek9Token(ctx.start), symbol);

    if (ctx.parameterisedType() != null) {
      final var resolved = checkClassSuitableToExtend.apply(ctx.parameterisedType());
      resolved.ifPresent(theSuper -> symbol.setSuperAggregate((IAggregateSymbol) theSuper));
    }

    if (ctx.traitsList() != null) {
      ctx.traitsList().traitReference().forEach(traitRef -> {
        final var resolved = checkClassTraitSuitableToExtend.apply(traitRef.identifierReference());
        resolved.ifPresent(theTrait -> symbol.addTrait((AggregateWithTraitsSymbol) theTrait));
      });
    }

    symbolsAndScopes.exitScope();

    super.exitDynamicClassDeclaration(ctx);
  }

  @Override
  public void enterDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Dynamic Function should have been defined", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterDynamicFunctionDeclaration(ctx);
  }

  /**
   * Dynamic functions can extend open/abstract normal functions, they can also (separately)
   * extends parameterised generic functions.
   */
  @Override
  public void exitDynamicFunctionDeclaration(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    final var symbol = (FunctionSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Dynamic Function should have been defined as symbol", symbol);

    if (ctx.identifierReference() != null) {
      final var resolved = checkFunctionSuitableToExtend.apply(ctx.identifierReference());
      resolved.ifPresent(theSuper -> symbol.setSuperFunction((FunctionSymbol) theSuper));
    } else if (ctx.parameterisedType() != null) {
      final var resolved = checkFunctionSuitableToExtend.apply(ctx.parameterisedType());
      resolved.ifPresent(theSuper -> symbol.setSuperFunction((FunctionSymbol) theSuper));
    }

    symbolsAndScopes.exitScope();

    super.exitDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void enterDynamicVariableCapture(final EK9Parser.DynamicVariableCaptureContext ctx) {

    final CaptureScope scope = (CaptureScope) symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Dynamic Variable Capture should have been defined", scope);
    scope.setOpenToEnclosingScope(true);
    symbolsAndScopes.enterScope(scope);

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

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Method should have been defined", scope);
    symbolsAndScopes.enterScope(scope);
    final var symbol = symbolsAndScopes.getRecordedSymbol(ctx);

    //This can be other types i.e. aggregate in case of Programs for example.
    if (symbol instanceof MethodSymbol methodSymbol && !methodSymbol.isConstructor()) {
      //Now this was not called in the first phase, and we only call it for non-constructors
      //(because they must have same name as a type)
      nameCollisionChecker.test(mostSpecificScope.get(), methodSymbol);
    }

    super.enterMethodDeclaration(ctx);
  }

  @Override
  public void exitMethodDeclaration(final EK9Parser.MethodDeclarationContext ctx) {

    if (ctx.identifierReference() != null) {
      checkApplicationForProgram.apply(ctx.identifierReference());
    }

    symbolsAndScopes.exitScope();

    super.exitMethodDeclaration(ctx);
  }

  @Override
  public void enterOperatorDeclaration(final EK9Parser.OperatorDeclarationContext ctx) {

    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    AssertValue.checkNotNull("Operator should have been defined", scope);
    symbolsAndScopes.enterScope(scope);

    super.enterOperatorDeclaration(ctx);
  }

  @Override
  public void exitOperatorDeclaration(final EK9Parser.OperatorDeclarationContext ctx) {

    final var currentScope = symbolsAndScopes.getTopScope();

    //Can be null if during definition 'enter' it was a duplicate operator
    if (currentScope instanceof MethodSymbol method) {
      //Yes this is correct an operator is just a method but marked as an operator.
      checkOperator.accept(method, ctx);
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
    final var currentScope = symbolsAndScopes.getTopScope();

    final ParseTree child =
        ctx.variableDeclaration() != null ? ctx.variableDeclaration() : ctx.variableOnlyDeclaration();
    final var symbol = symbolsAndScopes.getRecordedSymbol(child);
    if (currentScope instanceof MethodSymbol methodSymbol) {
      methodSymbol.setType(symbol.getType());
    }

  }

  @Override
  public void exitVariableOnlyDeclaration(final EK9Parser.VariableOnlyDeclarationContext ctx) {

    final var variableSymbol = symbolsAndScopes.getRecordedSymbol(ctx);
    final var theType = symbolsAndScopes.getRecordedSymbol(ctx.typeDef());
    if (theType != null) {
      variableSymbol.setType(theType);
    }

    if (ctx.BANG() != null) {
      checkNotGenericTypeParameter.accept(variableSymbol);
    }

    //While there is a check in phase one, this causes an ordering issue. So we run this in this phase.
    nameCollisionChecker.test(mostSpecificScope.get(), variableSymbol);

    super.exitVariableOnlyDeclaration(ctx);
  }

  @Override
  public void exitVariableDeclaration(final EK9Parser.VariableDeclarationContext ctx) {

    final var variableSymbol = symbolsAndScopes.getRecordedSymbol(ctx);
    if (ctx.typeDef() != null) {
      final var theType = symbolsAndScopes.getRecordedSymbol(ctx.typeDef());
      if (theType != null) {
        variableSymbol.setType(theType);
      }
    } else if (variableSymbol.getType().isEmpty()
        && (variableSymbol.isPropertyField() || variableSymbol.isReturningParameter())
        && ctx.assignmentExpression() != null) {

      processContextVariableDeclaration.accept(ctx);
    }

    //While there is a check in phase one, this causes an ordering issue. So we run this in this phase.
    nameCollisionChecker.test(mostSpecificScope.get(), variableSymbol);

    super.exitVariableDeclaration(ctx);
  }

  @Override
  public void exitRegisterStatement(final EK9Parser.RegisterStatementContext ctx) {

    //If there are any services registered then the application genus is modified from
    //being a GENERAL_APPLICATION to a SERVICE_APPLICATION.
    if (ctx.identifierReference() != null) {
      //Then it's component registration
      checkRegisterGenusValid.apply(ctx.identifierReference());
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

    setupGenericT.accept(ctx);

    super.exitParameterisedDetail(ctx);
  }

  @Override
  public void exitTypeDef(final EK9Parser.TypeDefContext ctx) {

    final var genericSymbols = accessGenericInGeneric.apply(symbolsAndScopes.getRecordedSymbol(ctx));
    genericSymbols.ifPresent(genericData -> genericData.parent().addGenericSymbolReference(genericData.dependent()));

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
    final var resolved = resolveOrDefineIdentifierReference.apply(ctx);
    resolved.ifPresent(symbol -> symbolsAndScopes.recordSymbol(symbol, ctx));

    super.enterIdentifierReference(ctx);
  }

  @Override
  public void enterTypeDef(final EK9Parser.TypeDefContext ctx) {

    final var resolved = resolveOrDefineTypeDef.apply(ctx);
    resolved.ifPresent(symbol -> symbolsAndScopes.recordSymbol(symbol, ctx));

    super.enterTypeDef(ctx);
  }

  @Override
  public void enterParameterisedType(final EK9Parser.ParameterisedTypeContext ctx) {

    final var resolved = resolveOrDefineExplicitParameterizedType.apply(ctx);
    resolved.ifPresent(symbol -> symbolsAndScopes.recordSymbol(symbol, ctx));

    super.enterParameterisedType(ctx);
  }

  private void processExtendableConstruct(final IToken token, final AggregateSymbol symbol,
                                          final EK9Parser.ExtendDeclarationContext extendDeclaration,
                                          final CheckSuitableToExtend extendChecker) {

    syntheticConstructorCreator.accept(symbol);
    checkVisibilityOfOperations.accept(symbol);
    checkForDuplicateOperations.accept(token, symbol);

    if (extendDeclaration != null) {
      final var resolved = extendChecker.apply(extendDeclaration.typeDef());
      resolved.ifPresent(theSuper -> symbol.setSuperAggregate((IAggregateSymbol) theSuper));
    }
  }

}


