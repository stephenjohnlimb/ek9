package org.ek9lang.compiler.main.phases.definition;

import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.antlr.EK9BaseListener;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.main.resolvedefine.ResolveOrDefineExplicitParameterizedType;
import org.ek9lang.compiler.main.resolvedefine.ResolveOrDefineIdentifierReference;
import org.ek9lang.compiler.main.resolvedefine.ResolveOrDefineTypeDef;
import org.ek9lang.compiler.main.rules.CheckNotGenericTypeParameter;
import org.ek9lang.compiler.main.rules.CheckSuitableGenus;
import org.ek9lang.compiler.main.rules.CheckSuitableToExtend;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbol.CaptureScope;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.ScopeStack;
import org.ek9lang.compiler.symbol.support.SymbolFactory;
import org.ek9lang.core.exception.AssertValue;

/**
 * A bit of a long-winded name, but this is really the second pass of the first phase of compilation.
 * The first pass will have defined lots of types, but in the case of explicit (non-inferred uses) of
 * template/generic types - definition will not have been possible during the first pass.
 * At the end of this source file there are basic lookups being done to record symbols against contexts.
 * Also, the association to types being extended could not be done in the very first pass.
 * So this pass also hooks up the super types/function - by resolving them.
 * It is important to do this 'supers' bit now - because the generic types can be referenced in bodies.
 * So as they are explicitly used in terms of 'T', 'K' and 'V' etc in subtypes/functions we need them
 * to be resolvable via the type/function hierarchy.
 * Note, we're not trying to resolve normal variables and parameters in this phase, but parametric types.
 * There's a reason everyone leaves out Generics/Templates - it's really hard.
 * It is now a hard fail if explicit type cannot be resolved. Not inferred types - not yet.
 * This is due to definition ordering and also the fact that each file is processed concurrently.
 * So, ordering is not guaranteed, the first pass - just accepts this and resolves/defines what it can.
 * But this second pass in the first phase will need to raise errors if it cannot resolve or define uses
 * of types/polymorphic parameterization - when it is declared and explicit (not inferred).
 * Phase 1 Definition first pass will have defined this or failed, and we won't even get this running.
 */
public class ResolveDefineExplicitTypeListener extends EK9BaseListener {
  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final ResolveOrDefineIdentifierReference resolveOrDefineIdentifierReference;
  private final ResolveOrDefineTypeDef resolveOrDefineTypeDef;
  private final ResolveOrDefineExplicitParameterizedType resolveOrDefineExplicitParameterizedType;

  private final CheckNotGenericTypeParameter checkNotGenericTypeParameter;

  private final CheckSuitableToExtend checkFunctionSuitableToExtend;

  private final CheckSuitableToExtend checkRecordSuitableToExtend;

  private final CheckSuitableToExtend checkClassTraitSuitableToExtend;

  private final CheckSuitableToExtend checkClassSuitableToExtend;

  private final CheckSuitableToExtend checkComponentSuitableToExtend;

  private final CheckSuitableGenus checkComponentToRegisterAgainst;

  private final CheckSuitableGenus checkAllowedClassSuitableGenus;

  private final CheckSuitableGenus checkApplicationForProgram;

  /**
   * Still in def phase 1 - but second pass to try and resolve types due to declaration ordering.
   */
  public ResolveDefineExplicitTypeListener(ParsedModule parsedModule) {
    //At construction the ScopeStack will push the module scope on to the stack.
    this.symbolAndScopeManagement = new SymbolAndScopeManagement(parsedModule,
        new ScopeStack(parsedModule.getModuleScope()));
    SymbolFactory symbolFactory = new SymbolFactory(parsedModule);

    var errorListener = parsedModule.getSource().getErrorListener();

    checkNotGenericTypeParameter = new CheckNotGenericTypeParameter(errorListener);

    /*
     * For identifier references we don't want errors issuing as we may traverse variables.
     * This phase is focussed on types.
     */
    resolveOrDefineIdentifierReference =
        new ResolveOrDefineIdentifierReference(symbolAndScopeManagement, symbolFactory, errorListener, false);

    /*
     * But for type defs, we do want errors because all types must be defined at this point in the phases
     * of compilation.
     */
    resolveOrDefineTypeDef =
        new ResolveOrDefineTypeDef(symbolAndScopeManagement, symbolFactory, errorListener, true);

    /*
     * Again we must have all the building blocks of types, so that parameterised types an be created.
     */
    resolveOrDefineExplicitParameterizedType =
        new ResolveOrDefineExplicitParameterizedType(symbolAndScopeManagement, symbolFactory, errorListener, true);

    checkFunctionSuitableToExtend =
        new CheckSuitableToExtend(symbolAndScopeManagement, errorListener,
            List.of(ISymbol.SymbolGenus.FUNCTION, ISymbol.SymbolGenus.FUNCTION_TRAIT), true);

    checkRecordSuitableToExtend =
        new CheckSuitableToExtend(symbolAndScopeManagement, errorListener, ISymbol.SymbolGenus.RECORD, false);

    checkClassTraitSuitableToExtend =
        new CheckSuitableToExtend(symbolAndScopeManagement, errorListener, ISymbol.SymbolGenus.CLASS_TRAIT, true);

    checkClassSuitableToExtend =
        new CheckSuitableToExtend(symbolAndScopeManagement, errorListener, ISymbol.SymbolGenus.CLASS, true);

    checkComponentSuitableToExtend =
        new CheckSuitableToExtend(symbolAndScopeManagement, errorListener, ISymbol.SymbolGenus.COMPONENT, true);

    checkComponentToRegisterAgainst =
        new CheckSuitableGenus(symbolAndScopeManagement, errorListener, ISymbol.SymbolGenus.COMPONENT, false, true);

    checkAllowedClassSuitableGenus =
        new CheckSuitableGenus(symbolAndScopeManagement, errorListener, ISymbol.SymbolGenus.CLASS, false, true);

    checkApplicationForProgram =
        new CheckSuitableGenus(symbolAndScopeManagement, errorListener,
            List.of(ISymbol.SymbolGenus.GENERAL_APPLICATION, ISymbol.SymbolGenus.SERVICE_APPLICATION), false, true);
  }

  /**
   * Process a function declaration, so this is at the very top level of construct definition.
   * This is NOT a dynamic function.
   */
  @Override
  public void enterFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Function should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterFunctionDeclaration(ctx);
  }

  @Override
  public void exitFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    var symbol = (FunctionSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Function should have been defined as symbol", symbol);

    //Normal functions can extend other normal functions if open/abstract - this code below checks.
    if (ctx.identifierReference() != null) {
      var resolved = checkFunctionSuitableToExtend.apply(ctx.identifierReference());
      resolved.ifPresent(theSuper -> symbol.setSuperFunctionSymbol((FunctionSymbol) theSuper));
    }
    symbolAndScopeManagement.exitScope();
    super.exitFunctionDeclaration(ctx);
  }

  @Override
  public void enterRecordDeclaration(EK9Parser.RecordDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Record should have been defined as scope", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterRecordDeclaration(ctx);
  }

  @Override
  public void exitRecordDeclaration(EK9Parser.RecordDeclarationContext ctx) {
    var symbol = (AggregateSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Record should have been defined as symbol", symbol);

    //In ek9, records can extend other records if open for extension
    if (ctx.extendDeclaration() != null) {
      var resolved = checkRecordSuitableToExtend.apply(ctx.extendDeclaration().typeDef());
      resolved.ifPresent(theSuper -> symbol.setSuperAggregateSymbol((IAggregateSymbol) theSuper));
    }

    symbolAndScopeManagement.exitScope();
    super.exitRecordDeclaration(ctx);
  }

  @Override
  public void enterTraitDeclaration(EK9Parser.TraitDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Trait should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterTraitDeclaration(ctx);
  }

  @Override
  public void exitTraitDeclaration(EK9Parser.TraitDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Trait should have been defined as symbol", symbol);

    if (ctx.traitsList() != null) {
      ctx.traitsList().traitReference().forEach(traitRef -> {
        var resolved = checkClassTraitSuitableToExtend.apply(traitRef.identifierReference());
        resolved.ifPresent(theTrait -> symbol.addTrait((AggregateWithTraitsSymbol) theTrait));
      });
    }

    if (ctx.allowingOnly() != null) {
      ctx.allowingOnly().identifierReference().forEach(classRef -> {
        var resolved = checkAllowedClassSuitableGenus.apply(classRef);
        resolved.ifPresent(theClass -> symbol.addAllowedExtender((IAggregateSymbol) theClass));
      });
    }

    symbolAndScopeManagement.exitScope();
    super.exitTraitDeclaration(ctx);
  }

  @Override
  public void enterClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Class should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterClassDeclaration(ctx);
  }

  @Override
  public void exitClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Class should have been defined as symbol", symbol);

    if (ctx.extendDeclaration() != null) {
      var resolved = checkClassSuitableToExtend.apply(ctx.extendDeclaration().typeDef());
      resolved.ifPresent(theSuper -> symbol.setSuperAggregateSymbol((IAggregateSymbol) theSuper));
    }

    if (ctx.traitsList() != null) {
      ctx.traitsList().traitReference().forEach(traitRef -> {
        var resolved = checkClassTraitSuitableToExtend.apply(traitRef.identifierReference());
        resolved.ifPresent(theTrait -> symbol.addTrait((AggregateWithTraitsSymbol) theTrait));
      });
    }
    symbolAndScopeManagement.exitScope();
    super.exitClassDeclaration(ctx);
  }

  @Override
  public void enterComponentDeclaration(EK9Parser.ComponentDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Component should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterComponentDeclaration(ctx);
  }

  @Override
  public void exitComponentDeclaration(EK9Parser.ComponentDeclarationContext ctx) {
    var symbol = (AggregateSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Component should have been defined as symbol", symbol);

    if (ctx.extendDeclaration() != null) {
      var resolved = checkComponentSuitableToExtend.apply(ctx.extendDeclaration().typeDef());
      resolved.ifPresent(theSuper -> symbol.setSuperAggregateSymbol((IAggregateSymbol) theSuper));
    }
    symbolAndScopeManagement.exitScope();
    super.exitComponentDeclaration(ctx);
  }

  @Override
  public void enterTextDeclaration(EK9Parser.TextDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Text should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterTextDeclaration(ctx);
  }

  @Override
  public void exitTextDeclaration(EK9Parser.TextDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitTextDeclaration(ctx);
  }

  @Override
  public void enterTextBodyDeclaration(EK9Parser.TextBodyDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Text Body should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterTextBodyDeclaration(ctx);
  }

  @Override
  public void exitTextBodyDeclaration(EK9Parser.TextBodyDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitTextBodyDeclaration(ctx);
  }

  @Override
  public void enterServiceDeclaration(EK9Parser.ServiceDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Service should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterServiceDeclaration(ctx);
  }

  @Override
  public void exitServiceDeclaration(EK9Parser.ServiceDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitServiceDeclaration(ctx);
  }

  @Override
  public void enterServiceOperationDeclaration(EK9Parser.ServiceOperationDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Service Operation should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterServiceOperationDeclaration(ctx);
  }

  @Override
  public void exitServiceOperationDeclaration(EK9Parser.ServiceOperationDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitServiceOperationDeclaration(ctx);
  }

  @Override
  public void enterApplicationDeclaration(EK9Parser.ApplicationDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Application should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterApplicationDeclaration(ctx);
  }

  @Override
  public void exitApplicationDeclaration(EK9Parser.ApplicationDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitApplicationDeclaration(ctx);
  }

  @Override
  public void enterDynamicClassDeclaration(EK9Parser.DynamicClassDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Dynamic Class should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterDynamicClassDeclaration(ctx);
  }

  /**
   * Dynamic classes can 'extend' a parameterised generic type.
   * But they can also (separately) implement one or more Traits.
   * They cannot extend a normal class, this is to promote composition.
   */
  @Override
  public void exitDynamicClassDeclaration(EK9Parser.DynamicClassDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Dynamic Class should have been defined as symbol", symbol);

    if (ctx.parameterisedType() != null) {
      var resolved = checkClassSuitableToExtend.apply(ctx.parameterisedType());
      resolved.ifPresent(theSuper -> symbol.setSuperAggregateSymbol((IAggregateSymbol) theSuper));
    }

    if (ctx.traitsList() != null) {
      ctx.traitsList().traitReference().forEach(traitRef -> {
        var resolved = checkClassTraitSuitableToExtend.apply(traitRef.identifierReference());
        resolved.ifPresent(theTrait -> symbol.addTrait((AggregateWithTraitsSymbol) theTrait));
      });
    }
    symbolAndScopeManagement.exitScope();
    super.exitDynamicClassDeclaration(ctx);
  }

  @Override
  public void enterDynamicFunctionDeclaration(EK9Parser.DynamicFunctionDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Dynamic Function should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterDynamicFunctionDeclaration(ctx);
  }

  /**
   * Dynamic functions can extend open/abstract normal functions, they can also (separately)
   * extends parameterised generic functions.
   */
  @Override
  public void exitDynamicFunctionDeclaration(EK9Parser.DynamicFunctionDeclarationContext ctx) {
    var symbol = (FunctionSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Dynamic Function should have been defined as symbol", symbol);

    if (ctx.identifierReference() != null) {
      var resolved = checkFunctionSuitableToExtend.apply(ctx.identifierReference());
      resolved.ifPresent(theSuper -> symbol.setSuperFunctionSymbol((FunctionSymbol) theSuper));
    } else if (ctx.parameterisedType() != null) {
      var resolved = checkFunctionSuitableToExtend.apply(ctx.parameterisedType());
      resolved.ifPresent(theSuper -> symbol.setSuperFunctionSymbol((FunctionSymbol) theSuper));
    }

    symbolAndScopeManagement.exitScope();
    super.exitDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void enterDynamicVariableCapture(EK9Parser.DynamicVariableCaptureContext ctx) {
    CaptureScope scope = (CaptureScope) symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Dynamic Variable Capture should have been defined", scope);
    scope.setOpenToEnclosingScope(true);
    symbolAndScopeManagement.enterScope(scope);
    super.enterDynamicVariableCapture(ctx);
  }

  @Override
  public void exitDynamicVariableCapture(EK9Parser.DynamicVariableCaptureContext ctx) {
    CaptureScope scope = (CaptureScope) symbolAndScopeManagement.getRecordedScope(ctx);
    scope.setOpenToEnclosingScope(false);
    symbolAndScopeManagement.exitScope();
    super.exitDynamicVariableCapture(ctx);
  }

  @Override
  public void enterMethodDeclaration(EK9Parser.MethodDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Method should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterMethodDeclaration(ctx);
  }

  @Override
  public void exitMethodDeclaration(EK9Parser.MethodDeclarationContext ctx) {
    if (ctx.identifierReference() != null) {
      checkApplicationForProgram.apply(ctx.identifierReference());
    }
    symbolAndScopeManagement.exitScope();
    super.exitMethodDeclaration(ctx);
  }

  @Override
  public void enterOperatorDeclaration(EK9Parser.OperatorDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Operator should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterOperatorDeclaration(ctx);
  }

  @Override
  public void exitOperatorDeclaration(EK9Parser.OperatorDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitOperatorDeclaration(ctx);
  }

  /**
   * It's important to reprocess this now, just to ensure that any returning types are defined on methods.
   * This is because it might not have been possible on the first pass - depending on ordering.
   * It might still not be set of the type cannot be resolved, but that's OK, other errors will pick that up.
   */
  @Override
  public void exitReturningParam(EK9Parser.ReturningParamContext ctx) {
    //Now get back to the parent scope, function, method, try, switch etc.
    super.exitReturningParam(ctx);
    var currentScope = symbolAndScopeManagement.getTopScope();

    ParseTree child = ctx.variableDeclaration() != null ? ctx.variableDeclaration() : ctx.variableOnlyDeclaration();
    var symbol = symbolAndScopeManagement.getRecordedSymbol(child);
    if (currentScope instanceof MethodSymbol methodSymbol) {
      methodSymbol.setType(symbol.getType());
    }
  }

  @Override
  public void exitVariableOnlyDeclaration(EK9Parser.VariableOnlyDeclarationContext ctx) {
    var variableSystem = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (ctx.BANG() != null) {
      checkNotGenericTypeParameter.accept(variableSystem);
    }
    if (ctx.typeDef() != null) {
      var theType = symbolAndScopeManagement.getRecordedSymbol(ctx.typeDef());
      if (theType != null) {
        variableSystem.setType(theType);
      }
    }

    super.exitVariableOnlyDeclaration(ctx);
  }

  @Override
  public void exitVariableDeclaration(EK9Parser.VariableDeclarationContext ctx) {
    var variableSystem = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (ctx.typeDef() != null) {
      var theType = symbolAndScopeManagement.getRecordedSymbol(ctx.typeDef());
      if (theType != null) {
        variableSystem.setType(theType);
      }
    }
    super.exitVariableDeclaration(ctx);
  }

  @Override
  public void exitRegisterStatement(EK9Parser.RegisterStatementContext ctx) {
    //Don't really do anything with the resolved component at the moment.
    if (ctx.identifierReference() != null) {
      checkComponentToRegisterAgainst.apply(ctx.identifierReference());
    }
    super.exitRegisterStatement(ctx);
  }

  /*
    -------------- Lookup and record items --------------------
    The lookup process here, will automatically issue semantic errors if there is no
    resolution. This will leave the ctx without any recorded symbol.
    This is important for other parts of ths phase as that means they must be aware
    that the ctx may not have a scope or symbol recorded against them.
  */

  @Override
  public void enterIdentifierReference(EK9Parser.IdentifierReferenceContext ctx) {
    //Now this is a bit different as we need to resolve in the current scope.
    var resolved = resolveOrDefineIdentifierReference.apply(ctx);
    resolved.ifPresent(symbol -> symbolAndScopeManagement.recordSymbol(symbol, ctx));
    super.enterIdentifierReference(ctx);
  }

  @Override
  public void enterTypeDef(EK9Parser.TypeDefContext ctx) {
    var resolved = resolveOrDefineTypeDef.apply(ctx);
    resolved.ifPresent(symbol -> symbolAndScopeManagement.recordSymbol(symbol, ctx));

    super.enterTypeDef(ctx);
  }

  @Override
  public void enterParameterisedType(EK9Parser.ParameterisedTypeContext ctx) {

    var resolved = resolveOrDefineExplicitParameterizedType.apply(ctx);
    resolved.ifPresent(symbol -> symbolAndScopeManagement.recordSymbol(symbol, ctx));

    super.enterParameterisedType(ctx);
  }
}


