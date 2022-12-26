package org.ek9lang.compiler.main.phases.listeners;

import org.ek9lang.antlr.EK9BaseListener;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.support.ScopeStack;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * The abstract base on most antlr listeners. This class does little except ensure that
 * By the way ANTLR is designed we have to listen to events and push
 * our constructed symbols into a stack and also pop them off again.
 * As they are processed we also have to record them in a more permanent manner.
 * So the stack is used to help build the aggregates etc. But in the end
 * they are all 'popped off' - our main permanent holding area is the parsedModule!
 */
public abstract class AbstractEK9PhaseListener extends EK9BaseListener {

  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final ParsedModule parsedModule;

  protected final SymbolAndScopeManagement symbolAndScopeManagement;

  protected AbstractEK9PhaseListener(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                  ParsedModule parsedModule) {
    AssertValue.checkNotNull("CompilableProgramAccess cannot be null", compilableProgramAccess);
    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);

    this.compilableProgramAccess = compilableProgramAccess;
    this.parsedModule = parsedModule;

    //At construction the ScopeStack will push the module scope on to the stack.
    this.symbolAndScopeManagement = new SymbolAndScopeManagement(parsedModule,
        new ScopeStack(parsedModule.getModuleScope()));
  }

  /**
   * Provide access to the parsedModule.
   */
  protected ParsedModule getParsedModule() {
    return parsedModule;
  }

  /**
   * Provide access to errorListener to extending Listeners.
   * This enables reporting of errors and warnings.
   */
  public ErrorListener getErrorListener() {
    return parsedModule.getSource().getErrorListener();
  }

  public boolean isScopeStackEmpty() {
    return symbolAndScopeManagement.getTopScope() == null;
  }

  @Override
  public void exitModuleDeclaration(EK9Parser.ModuleDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitModuleDeclaration(ctx);
  }

  @Override
  public void exitMethodDeclaration(EK9Parser.MethodDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitMethodDeclaration(ctx);
  }

  @Override
  public void exitOperatorDeclaration(EK9Parser.OperatorDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitOperatorDeclaration(ctx);
  }

  @Override
  public void exitFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitFunctionDeclaration(ctx);
  }

  @Override
  public void exitRecordDeclaration(EK9Parser.RecordDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitRecordDeclaration(ctx);
  }

  @Override
  public void exitTraitDeclaration(EK9Parser.TraitDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitTraitDeclaration(ctx);
  }

  @Override
  public void exitClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitClassDeclaration(ctx);
  }

  @Override
  public void exitComponentDeclaration(EK9Parser.ComponentDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitComponentDeclaration(ctx);
  }

  @Override
  public void exitTextDeclaration(EK9Parser.TextDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitTextDeclaration(ctx);
  }

  @Override
  public void exitTextBodyDeclaration(EK9Parser.TextBodyDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitTextBodyDeclaration(ctx);
  }

  @Override
  public void exitServiceDeclaration(EK9Parser.ServiceDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitServiceDeclaration(ctx);
  }

  @Override
  public void exitServiceOperationDeclaration(EK9Parser.ServiceOperationDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitServiceOperationDeclaration(ctx);
  }

  @Override
  public void exitApplicationDeclaration(EK9Parser.ApplicationDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitApplicationDeclaration(ctx);
  }

  @Override
  public void exitDynamicClassDeclaration(EK9Parser.DynamicClassDeclarationContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitDynamicClassDeclaration(ctx);
  }

  @Override
  public void exitTypeDeclaration(EK9Parser.TypeDeclarationContext ctx) {
    if (ctx.Identifier() != null) {
      symbolAndScopeManagement.exitScope();
    }
    super.exitTypeDeclaration(ctx);
  }

  @Override
  public void exitInstructionBlock(EK9Parser.InstructionBlockContext ctx) {
    symbolAndScopeManagement.exitScope();
    super.exitInstructionBlock(ctx);
  }
}
