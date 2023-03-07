package org.ek9lang.compiler.main.phases.definition;

import org.ek9lang.antlr.EK9BaseListener;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.main.resolvedefine.ResolveOrDefineExplicitParameterizedType;
import org.ek9lang.compiler.main.resolvedefine.ResolveOrDefineTypeDef;
import org.ek9lang.compiler.symbol.support.ScopeStack;
import org.ek9lang.compiler.symbol.support.SymbolFactory;
import org.ek9lang.core.exception.AssertValue;

/**
 * A bit of a long-winded name, but this is really the second pass of the first phase of compilation.
 * The first pass will have defined lots of types, but in the case of explicit (non-inferred uses) of
 * template/generic types - definition might not have been possible during the first pass.
 * This is due to definition ordering and also the fact that each file is processed concurrently.
 * So, ordering is not guaranteed, the first pass - just accepts this and resolves/defines what it can.
 * But this second pass in the first phase will need to raise errors if it cannot resolve or define uses
 * of polymorphic parameterization - when it is declared and explicit (not inferred).
 * Phase 1 Definition first pass will have defined this or failed and we won't even get this running.
 */
public class ResolveDefineExplicitTemplateUseListener extends EK9BaseListener {
  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final ResolveOrDefineTypeDef resolveOrDefineTypeDef;
  private final ResolveOrDefineExplicitParameterizedType resolveOrDefineExplicitParameterizedType;

  /**
   * Still in def phase 1 - but second pass to try and resolve types due to declaration ordering.
   */
  public ResolveDefineExplicitTemplateUseListener(ParsedModule parsedModule) {
    //At construction the ScopeStack will push the module scope on to the stack.
    this.symbolAndScopeManagement = new SymbolAndScopeManagement(parsedModule,
        new ScopeStack(parsedModule.getModuleScope()));
    SymbolFactory symbolFactory = new SymbolFactory(parsedModule);

    var errorListener = parsedModule.getSource().getErrorListener();

    resolveOrDefineTypeDef =
        new ResolveOrDefineTypeDef(symbolAndScopeManagement, symbolFactory, errorListener, true);

    resolveOrDefineExplicitParameterizedType =
        new ResolveOrDefineExplicitParameterizedType(symbolAndScopeManagement, symbolFactory, errorListener, true);
  }

  @Override
  public void enterFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Function should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterFunctionDeclaration(ctx);
  }

  @Override
  public void exitFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitFunctionDeclaration(ctx);
  }

  @Override
  public void enterRecordDeclaration(EK9Parser.RecordDeclarationContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Record should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterRecordDeclaration(ctx);
  }

  @Override
  public void exitRecordDeclaration(EK9Parser.RecordDeclarationContext ctx) {
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

  @Override
  public void exitDynamicClassDeclaration(EK9Parser.DynamicClassDeclarationContext ctx) {
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

  @Override
  public void exitDynamicFunctionDeclaration(EK9Parser.DynamicFunctionDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitDynamicFunctionDeclaration(ctx);
  }

  @Override
  public void enterDynamicVariableCapture(EK9Parser.DynamicVariableCaptureContext ctx) {
    var scope = symbolAndScopeManagement.getRecordedScope(ctx);
    AssertValue.checkNotNull("Dynamic Variable Capture should have been defined", scope);
    symbolAndScopeManagement.enterScope(scope);
    super.enterDynamicVariableCapture(ctx);
  }

  @Override
  public void exitDynamicVariableCapture(EK9Parser.DynamicVariableCaptureContext ctx) {
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

  @Override
  public void enterTypeDef(EK9Parser.TypeDefContext ctx) {
    resolveOrDefineTypeDef.apply(ctx);
  }

  @Override
  public void enterParameterisedType(EK9Parser.ParameterisedTypeContext ctx) {
    //Nothing is done with the return here
    resolveOrDefineExplicitParameterizedType.apply(ctx);
  }
}


