package org.ek9lang.compiler.phase7.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.DebugInfo;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

/**
 * Central state management for IR generation, following the proven SymbolsAndScopes pattern.
 *
 * <p>This context provides stack-based scope management that eliminates parameter threading
 * throughout the IR generation process. It manages the transient stack of scope contexts,
 * provides debug information management, and handles instruction building coordination.</p>
 *
 * <p>Like SymbolsAndScopes, this class provides the shared state management that allows
 * focused helpers to work together cleanly without complex parameter passing.</p>
 */
public class IRGenerationContext {

  private final IRContext irContext;
  private final IRGenerationStack irGenerationStack;
  private final DebugInfoCreator debugInfoCreator;
  private final List<IRInstr> currentInstructions;

  /**
   * Create a new IR generation context with existing IRContext.
   * This preserves the original IRContext state including prefix counters.
   */
  public IRGenerationContext(final IRContext irContext) {
    AssertValue.checkNotNull("IRContext cannot be null", irContext);

    this.irContext = irContext;
    this.debugInfoCreator = new DebugInfoCreator(irContext);
    this.currentInstructions = new ArrayList<>();

    // Initialize stack with module-level base frame
    var moduleFrame = IRStackFrame.basic("module", null, IRFrameType.MODULE);
    this.irGenerationStack = new IRGenerationStack(moduleFrame);
  }

  /**
   * Enter a new IR scope context.
   */
  public void enterScope(String scopeId, DebugInfo debugInfo, IRFrameType frameType) {
    var frame = IRStackFrame.basic(scopeId, debugInfo, frameType);
    irGenerationStack.push(frame);
  }

  /**
   * Enter a method/function scope with fresh IRContext for counter isolation.
   * Creates new IRContext via copy constructor to ensure fresh temp var and label counters.
   */
  public void enterMethodScope(String scopeId, DebugInfo debugInfo, IRFrameType frameType) {
    // Create fresh IRContext for method-level counter isolation
    var methodIRContext = new IRContext(irContext);
    var frame = IRStackFrame.withContext(scopeId, debugInfo, frameType, methodIRContext);
    irGenerationStack.push(frame);
  }

  /**
   * Enter a new IR scope with left-hand side indication.
   */
  public void enterScope(String scopeId, DebugInfo debugInfo, IRFrameType frameType, boolean hasLeftHandSide) {
    var frame = IRStackFrame.withLeftHandSide(scopeId, debugInfo, frameType, hasLeftHandSide);
    irGenerationStack.push(frame);
  }

  /**
   * Enter a new IR scope with context data.
   */
  public void enterScope(String scopeId, DebugInfo debugInfo, IRFrameType frameType, Object contextData) {
    var frame = IRStackFrame.withContext(scopeId, debugInfo, frameType, contextData);
    irGenerationStack.push(frame);
  }

  /**
   * Enter a new IR scope with full parameters.
   */
  public void enterScope(String scopeId, DebugInfo debugInfo, IRFrameType frameType,
                         boolean hasLeftHandSide, Object contextData) {
    var frame = IRStackFrame.full(scopeId, debugInfo, frameType, hasLeftHandSide, contextData);
    irGenerationStack.push(frame);
  }

  /**
   * Exit the current IR scope.
   */
  public void exitScope() {
    irGenerationStack.pop();
  }

  /**
   * Get current scope ID from the stack.
   */
  public String currentScopeId() {
    return irGenerationStack.currentScopeId();
  }

  /**
   * Get current debug info from the stack.
   */
  public Optional<DebugInfo> currentDebugInfo() {
    return irGenerationStack.currentDebugInfo();
  }

  /**
   * Check if current scope has left-hand side.
   */
  public boolean hasLeftHandSide() {
    return irGenerationStack.hasLeftHandSide();
  }

  /**
   * Get current frame type.
   */
  public IRFrameType currentFrameType() {
    return irGenerationStack.currentFrameType();
  }

  /**
   * Create debug info from a parse context.
   */
  public DebugInfo createDebugInfo(final ParseTree ctx) {
    if (ctx instanceof ParserRuleContext parserCtx && parserCtx.start != null) {
      return debugInfoCreator.apply(new Ek9Token(parserCtx.start));
    }

    return null;
  }

  /**
   * Create debug info from a token.
   */
  public DebugInfo createDebugInfo(final Ek9Token token) {
    return debugInfoCreator.apply(token);
  }

  /**
   * Create debug info from an ANTLR token.
   */
  public DebugInfo createDebugInfo(final Token token) {
    return debugInfoCreator.apply(new Ek9Token(token));
  }

  /**
   * Create debug info from an EK9 IToken.
   */
  public DebugInfo createDebugInfo(final IToken token) {
    if (token instanceof Ek9Token ek9Token) {
      return debugInfoCreator.apply(ek9Token);
    } else {
      // If it's not an Ek9Token, we need to create one from the IToken's properties
      // This handles cases where we have other IToken implementations
      return debugInfoCreator.apply(new Ek9Token(token.getType(), token.getText(),
          token.getLine(), token.getSourceName(),
          token.getCharPositionInLine(), token.getTokenIndex()));
    }
  }

  /**
   * Get current IRContext from stack frame (traverses up to find method-level IRContext).
   * Always creates a fresh IRContext for proper counter isolation if none exists.
   */
  public IRContext getCurrentIRContext() {
    // Traverse up the stack to find the first frame with IRContext (method-level)
    var methodContext = irGenerationStack.traverseBackToContextData(IRContext.class);
    return methodContext.orElse(irContext);

    // If no method-level context found, this indicates a problem with scope management
    // For now, return the base context to avoid creating fresh contexts on every call
    // This should not happen if enterMethodScope() is called correctly
  }

  /**
   * Generate a unique temporary variable name.
   */
  public String generateTempName() {
    return getCurrentIRContext().generateTempName();
  }

  /**
   * Generate a unique scope identifier.
   */
  public String generateScopeId(String prefix) {
    return getCurrentIRContext().generateScopeId(prefix);
  }

  /**
   * Generate a unique block label.
   */
  public String generateBlockLabel(String prefix) {
    return getCurrentIRContext().generateBlockLabel(prefix);
  }

  /**
   * Generate a unique label name.
   */
  public String generateLabelName(String prefix) {
    return getCurrentIRContext().generateLabelName(prefix);
  }

  /**
   * Create BasicDetails using current stack context.
   */
  public BasicDetails createBasicDetails() {
    return new BasicDetails(currentScopeId(), currentDebugInfo().orElse(null));
  }

  /**
   * Create BasicDetails with custom scope ID but current debug info.
   */
  public BasicDetails createBasicDetails(String scopeId) {
    return new BasicDetails(scopeId, currentDebugInfo().orElse(null));
  }

  /**
   * Create VariableDetails using current stack context.
   */
  public VariableDetails createVariableDetails(String variableName) {
    return new VariableDetails(variableName, createBasicDetails());
  }

  /**
   * Create VariableDetails with custom scope ID.
   */
  public VariableDetails createVariableDetails(String variableName, String scopeId) {
    return new VariableDetails(variableName, createBasicDetails(scopeId));
  }

  /**
   * Add instruction to current instruction list.
   */
  public void addInstruction(IRInstr instruction) {
    currentInstructions.add(instruction);
  }

  /**
   * Add multiple instructions to current instruction list.
   */
  public void addInstructions(List<IRInstr> instructions) {
    currentInstructions.addAll(instructions);
  }

  /**
   * Get current instruction list and clear it.
   */
  public List<IRInstr> extractInstructions() {
    var instructions = new ArrayList<>(currentInstructions);
    currentInstructions.clear();
    return instructions;
  }

  /**
   * Get current instruction list without clearing.
   */
  public List<IRInstr> getCurrentInstructions() {
    return new ArrayList<>(currentInstructions);
  }

  /**
   * Clear current instruction list.
   */
  public void clearInstructions() {
    currentInstructions.clear();
  }

  /**
   * Check if we need a result variable for the current context.
   */
  public boolean needsResultVariable(String returnType) {
    return !"org.ek9.lang::Void".equals(returnType) && hasLeftHandSide();
  }

  /**
   * Navigate back up stack to find frame of specific type.
   */
  public Optional<IRStackFrame> findFrameOfType(IRFrameType frameType) {
    return irGenerationStack.traverseBackToFrameType(frameType);
  }

  /**
   * Navigate back up stack to find method or function frame.
   */
  public Optional<IRStackFrame> findMethodOrFunctionFrame() {
    return irGenerationStack.traverseBackToMethodOrFunction();
  }

  /**
   * Navigate back up stack to find aggregate frame.
   */
  public Optional<IRStackFrame> findAggregateFrame() {
    return irGenerationStack.traverseBackToAggregate();
  }

  /**
   * Check if we're currently in a specific frame type.
   */
  public boolean isInFrameType(IRFrameType frameType) {
    return irGenerationStack.isInFrameType(frameType);
  }

  /**
   * Get the parsed module from IRContext.
   */
  public ParsedModule getParsedModule() {
    return irContext.getParsedModule();
  }

  /**
   * Get the compiler flags from IRContext.
   */
  public CompilerFlags getCompilerFlags() {
    return irContext.getCompilerFlags();
  }

  /**
   * Get the underlying IR context.
   */
  public IRContext getIRContext() {
    return irContext;
  }

  /**
   * Get the IR generation stack (for advanced operations).
   */
  public IRGenerationStack getIRGenerationStack() {
    return irGenerationStack;
  }

  /**
   * Get current stack depth (for debugging).
   */
  public int getStackDepth() {
    return irGenerationStack.depth();
  }
}