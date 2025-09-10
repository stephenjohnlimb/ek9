package org.ek9lang.compiler.phase7;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.AbstractEK9PhaseListener;
import org.ek9lang.compiler.phase7.support.IRFrameType;
import org.ek9lang.compiler.phase7.support.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRInstructionBuilder;

/**
 * Main coordinator for IR generation, following the proven EK9 phase listener pattern.
 * 
 * <p>Like ExpressionsListener orchestrates focused helpers for phase 3, this coordinator
 * orchestrates focused IR generation helpers for phase 7, providing clean separation
 * of concerns and eliminating parameter threading hell.</p>
 * 
 * <p>Uses the stack-based IRGenerationContext to manage scope, debug info, and
 * instruction building consistently across all helpers.</p>
 * 
 * <p>Note: This is a foundation class with placeholder implementations. Many methods
 * are commented out or have TODO markers while the EK9Parser API is better understood.</p>
 */
public class IRGenerationCoordinator extends AbstractEK9PhaseListener {

  private final IRGenerationContext irGenerationContext;
  private final IRInstructionBuilder instructionBuilder;

  // Focused helpers - each does ONE thing well
  private final SyntheticMethodIRHelper syntheticMethodHelper;
  private final EnumerationIRHelper enumerationHelper;
  private final ExpressionIRHelper expressionHelper;
  private final StatementIRHelper statementHelper;
  private final CallIRHelper callHelper;
  
  // Advanced synthesis helpers (for future features)
  private final InjectionPointIRHelper injectionHelper;
  private final AspectWeavingIRHelper aspectHelper;
  private final ApplicationBindingIRHelper appBindingHelper;

  /**
   * Create the IR generation coordinator with an existing IRContext.
   * The IRContext should already contain the proper ParsedModule and CompilerFlags
   * from the compilation pipeline.
   */
  protected IRGenerationCoordinator(final org.ek9lang.compiler.phase7.support.IRContext irContext) {
    super(irContext.getParsedModule());

    // Use the provided IRContext with its existing CompilerFlags
    this.irGenerationContext = new IRGenerationContext(irContext);
    this.instructionBuilder = new IRInstructionBuilder(irGenerationContext);

    // Initialize focused helpers
    this.syntheticMethodHelper = new SyntheticMethodIRHelper(irGenerationContext, instructionBuilder);
    this.enumerationHelper = new EnumerationIRHelper(irGenerationContext, instructionBuilder);
    this.expressionHelper = new ExpressionIRHelper(irGenerationContext, instructionBuilder);
    this.statementHelper = new StatementIRHelper(irGenerationContext, instructionBuilder);
    this.callHelper = new CallIRHelper(irGenerationContext, instructionBuilder);
    
    // Advanced synthesis helpers
    this.injectionHelper = new InjectionPointIRHelper(irGenerationContext, instructionBuilder);
    this.aspectHelper = new AspectWeavingIRHelper(irGenerationContext, instructionBuilder);
    this.appBindingHelper = new ApplicationBindingIRHelper(irGenerationContext, instructionBuilder);
  }

  // ===== CLASS AND RECORD DEFINITIONS =====

  @Override
  public void enterClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    var debugInfo = irGenerationContext.createDebugInfo(ctx);
    var scopeId = generateScopeId("class", ctx);
    irGenerationContext.enterScope(scopeId, debugInfo, IRFrameType.CLASS);
  }

  @Override
  public void exitClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    var classSymbol = symbolsAndScopes.getRecordedSymbol(ctx);
    
    // Clean orchestration - each helper handles its domain
    if (hasSyntheticMethods(classSymbol)) {
      syntheticMethodHelper.generateFor(classSymbol);
    }
    
    if (hasEnumerationOperators(classSymbol)) {
      enumerationHelper.generateFor(classSymbol);
    }
    
    if (hasInjectionPoints(classSymbol)) {
      injectionHelper.generateFor(classSymbol);
    }
    
    if (hasAspects(classSymbol)) {
      aspectHelper.generateFor(classSymbol);
    }
    
    irGenerationContext.exitScope();
  }

  @Override
  public void enterRecordDeclaration(EK9Parser.RecordDeclarationContext ctx) {
    var debugInfo = irGenerationContext.createDebugInfo(ctx);
    var scopeId = generateScopeId("record", ctx);
    irGenerationContext.enterScope(scopeId, debugInfo, IRFrameType.RECORD);
  }

  @Override
  public void exitRecordDeclaration(EK9Parser.RecordDeclarationContext ctx) {
    var recordSymbol = symbolsAndScopes.getRecordedSymbol(ctx);
    
    // Records get synthetic operators too
    if (hasSyntheticMethods(recordSymbol)) {
      syntheticMethodHelper.generateFor(recordSymbol);
    }
    
    irGenerationContext.exitScope();
  }

  // Note: Function declaration methods exist and work
  @Override
  public void enterFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    var debugInfo = irGenerationContext.createDebugInfo(ctx);
    var scopeId = generateScopeId("function", ctx);
    irGenerationContext.enterScope(scopeId, debugInfo, IRFrameType.FUNCTION);
  }

  @Override
  public void exitFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    var functionSymbol = symbolsAndScopes.getRecordedSymbol(ctx);
    
    // Generate IR for function body
    // TODO: Check correct method to get instruction block
    // if (ctx.instructionBlock() != null) {
    //   statementHelper.generateFor(ctx.instructionBlock());
    // }
    
    irGenerationContext.exitScope();
  }

  // ===== EXPRESSIONS AND STATEMENTS =====

  @Override
  public void enterExpression(EK9Parser.ExpressionContext ctx) {
    var debugInfo = irGenerationContext.createDebugInfo(ctx);
    var scopeId = generateScopeId("expr", ctx);
    irGenerationContext.enterScope(scopeId, debugInfo, IRFrameType.EXPRESSION);
  }

  @Override
  public void exitExpression(EK9Parser.ExpressionContext ctx) {
    expressionHelper.generateFor(ctx);
    irGenerationContext.exitScope();
  }

  @Override
  public void enterCall(EK9Parser.CallContext ctx) {
    var debugInfo = irGenerationContext.createDebugInfo(ctx);
    var scopeId = generateScopeId("call", ctx);
    irGenerationContext.enterScope(scopeId, debugInfo, IRFrameType.CALL);
  }

  @Override
  public void exitCall(EK9Parser.CallContext ctx) {
    callHelper.generateFor(ctx);
    irGenerationContext.exitScope();
  }

  @Override
  public void enterAssignmentStatement(EK9Parser.AssignmentStatementContext ctx) {
    var debugInfo = irGenerationContext.createDebugInfo(ctx);
    var scopeId = generateScopeId("assign", ctx);
    irGenerationContext.enterScope(scopeId, debugInfo, IRFrameType.ASSIGNMENT, true); // Has LHS
  }

  @Override
  public void exitAssignmentStatement(EK9Parser.AssignmentStatementContext ctx) {
    statementHelper.generateAssignment(ctx);
    irGenerationContext.exitScope();
  }

  @Override
  public void enterBlock(EK9Parser.BlockContext ctx) {
    var debugInfo = irGenerationContext.createDebugInfo(ctx);
    var scopeId = generateScopeId("block", ctx);
    irGenerationContext.enterScope(scopeId, debugInfo, IRFrameType.BLOCK);
  }

  @Override
  public void exitBlock(EK9Parser.BlockContext ctx) {
    statementHelper.generateBlock(ctx);
    irGenerationContext.exitScope();
  }

  // ===== APPLICATION DEFINITIONS =====

  @Override
  public void enterApplicationDeclaration(EK9Parser.ApplicationDeclarationContext ctx) {
    var debugInfo = irGenerationContext.createDebugInfo(ctx);
    var scopeId = generateScopeId("application", ctx);
    irGenerationContext.enterScope(scopeId, debugInfo, IRFrameType.APPLICATION);
  }

  @Override
  public void exitApplicationDeclaration(EK9Parser.ApplicationDeclarationContext ctx) {
    var appSymbol = symbolsAndScopes.getRecordedSymbol(ctx);
    appBindingHelper.generateFor(appSymbol);
    irGenerationContext.exitScope();
  }

  // ===== HELPER METHODS =====

  private String generateScopeId(String prefix, org.antlr.v4.runtime.tree.ParseTree ctx) {
    // Generate unique scope ID using context hash for consistency
    return irGenerationContext.generateScopeId(prefix + "_" + ctx.hashCode());
  }

  private boolean hasSyntheticMethods(Object symbol) {
    // Check if symbol has synthetic methods that need IR generation
    // This would use existing markers from earlier phases
    return true; // Placeholder - implement based on symbol type
  }

  private boolean hasEnumerationOperators(Object symbol) {
    // Check if symbol is enumeration type needing operator synthesis
    return false; // Placeholder - implement based on symbol type
  }

  private boolean hasInjectionPoints(Object symbol) {
    // Check if symbol has dependency injection points (! suffix)
    return false; // Placeholder - implement based on symbol analysis
  }

  private boolean hasAspects(Object symbol) {
    // Check if symbol has AOP aspects to weave
    return false; // Placeholder - implement based on application context
  }

  /**
   * Get the IR generation context (for testing and debugging).
   */
  public IRGenerationContext getIrGenerationContext() {
    return irGenerationContext;
  }

  /**
   * Get the instruction builder (for testing and debugging).
   */
  public IRInstructionBuilder getInstructionBuilder() {
    return instructionBuilder;
  }
}