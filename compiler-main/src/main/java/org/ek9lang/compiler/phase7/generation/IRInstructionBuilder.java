package org.ek9lang.compiler.phase7.generation;

import java.util.List;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.instructions.BasicBlockInstr;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.LabelInstr;
import org.ek9lang.compiler.ir.instructions.LiteralInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.calls.CallDetailsBuilder;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Stateful instruction builder that uses the IR generation context automatically.
 *
 * <p>This builder eliminates the need for manual BasicDetails construction and
 * scope/debug info management by using the current stack context from
 * IRGenerationContext automatically.</p>
 *
 * <p>All instruction creation methods use the current scope and debug information
 * from the stack, ensuring consistency and eliminating parameter threading.</p>
 */
public class IRInstructionBuilder {

  private final IRGenerationContext context;

  /**
   * Create a new instruction builder with stack-based context access.
   * This enables generators to access the current IRContext without parameter threading.
   */
  public IRInstructionBuilder(IRGenerationContext context) {
    this.context = context;
  }

  public DebugInfo createDebugInfo(final ParseTree ctx) {
    return context.createDebugInfo(ctx);
  }

  /**
   * Create debug info from a token.
   */
  public DebugInfo createDebugInfo(final Ek9Token token) {
    return context.createDebugInfo(token);
  }

  /**
   * Create debug info from an ANTLR token.
   */
  public DebugInfo createDebugInfo(final Token token) {
    return context.createDebugInfo(new Ek9Token(token));
  }

  /**
   * Create debug info from an EK9 IToken.
   */
  public DebugInfo createDebugInfo(final IToken token) {
    return context.createDebugInfo(token);
  }

  /**
   * Create a temporary variable name.
   */
  public String createTempVariable() {
    return context.generateTempName();
  }

  /**
   * Create a label instruction using current context.
   */
  public LabelInstr createLabel(String labelPrefix) {
    var label = context.generateLabelName(labelPrefix);
    var instruction = LabelInstr.label(label);
    context.addInstruction(instruction);
    return instruction;
  }

  /**
   * Load literal instruction using current context.
   */
  public void loadLiteral(String variableName, String literalValue, String literalType) {
    var debugInfo = context.currentDebugInfo().orElse(null);
    var instruction = LiteralInstr.literal(variableName, literalValue, literalType, debugInfo);
    context.addInstruction(instruction);
  }

  /**
   * Create a call instruction using current context.
   */
  public CallInstr createCall(CallContext callContext) {
    var callDetailsBuilder = new CallDetailsBuilder(context);
    var callDetailsResult = callDetailsBuilder.apply(callContext);
    var debugInfo = context.currentDebugInfo().orElse(null);
    var instruction = CallInstr.call(null, debugInfo, callDetailsResult.callDetails());
    context.addInstruction(instruction);
    return instruction;
  }

  /**
   * Create a call instruction with target variable using current context.
   * Handles void vs non-void returns automatically.
   */
  public void callWithTarget(String targetVariable, CallDetails callDetails) {
    var debugInfo = context.currentDebugInfo().orElse(null);
    var instruction = CallInstr.call(targetVariable, debugInfo, callDetails);
    context.addInstruction(instruction);
  }

  /**
   * Create a static call instruction using current context.
   */
  public void callStatic(String targetVariable, CallDetails callDetails) {
    var debugInfo = context.currentDebugInfo().orElse(null);
    var instruction = CallInstr.callStatic(targetVariable, debugInfo, callDetails);
    context.addInstruction(instruction);
  }

  /**
   * Create a function call with automatic void/non-void handling.
   * Adds promotion instructions and creates the call instruction.
   */
  public CallInstr createFunctionCall(String targetVariable, CallDetailsBuilder.CallDetailsResult callDetailsResult) {
    // Add any promotion instructions first
    addInstructions(callDetailsResult.allInstructions());

    // Create the call with proper void handling
    var debugInfo = context.currentDebugInfo().orElse(null);
    var targetVar = "org.ek9.lang::Void".equals(callDetailsResult.callDetails().returnTypeName())
        ? null : targetVariable;

    var instruction = CallInstr.call(targetVar, debugInfo, callDetailsResult.callDetails());
    context.addInstruction(instruction);
    return instruction;
  }

  /**
   * Create a method call to super class constructor or method.
   */
  public void callSuperMethod(ISymbol superType, String methodName) {
    var metaData = CallMetaDataDetails.defaultMetaData();

    var callDetails = new CallDetails(
        IRConstants.SUPER,
        superType.getFullyQualifiedName(),
        methodName,
        List.of(),
        superType.getFullyQualifiedName(),
        List.of(),
        metaData,
        false
    );

    callWithTarget(IRConstants.TEMP_SUPER_INIT, callDetails);
  }

  /**
   * Create a static call to super class initialization method.
   */
  public void callSuperStaticMethod(ISymbol superType, String methodName) {
    var metaData = CallMetaDataDetails.defaultMetaData();
    var voidStr = context.getCurrentIRContext().getParsedModule().getEk9Types().ek9Void().getFullyQualifiedName();

    var callDetails = new CallDetails(
        null,
        superType.getFullyQualifiedName(),
        methodName,
        List.of(),
        voidStr,
        List.of(),
        metaData,
        false
    );

    callStatic(IRConstants.TEMP_C_INIT, callDetails);
  }

  /**
   * Create a method call on 'this' object.
   */
  public void callThisMethod(String targetTypeName, String methodName, String targetVariable) {
    callThisMethod(targetTypeName, methodName, targetVariable, null);
  }

  /**
   * Create a method call on 'this' object with specific debug info.
   */
  public void callThisMethod(String targetTypeName, String methodName, String targetVariable, DebugInfo debugInfo) {
    var metaData = CallMetaDataDetails.defaultMetaData();
    var voidStr = context.getCurrentIRContext().getParsedModule().getEk9Types().ek9Void().getFullyQualifiedName();

    var callDetails = new CallDetails(
        IRConstants.THIS,
        targetTypeName,
        methodName,
        List.of(),
        voidStr,
        List.of(),
        metaData,
        false
    );

    var finalDebugInfo = debugInfo != null ? debugInfo : context.currentDebugInfo().orElse(null);
    var instruction = CallInstr.call(targetVariable, finalDebugInfo, callDetails);
    context.addInstruction(instruction);
  }

  /**
   * Create a BasicBlock with given label and current instructions.
   */
  public BasicBlockInstr createBasicBlock(String labelPrefix) {
    var blockLabel = context.generateBlockLabel(labelPrefix);
    var instructions = extractInstructions();
    return new BasicBlockInstr(blockLabel).addInstructions(instructions);
  }


  /**
   * Create a memory retain and register for memory management current context.
   */
  public void manageVariable(String variableName) {
    final var debugInfo = context.currentDebugInfo().orElse(null);
    final var scopeId = context.currentScopeId();
    var retain = MemoryInstr.retain(variableName, context.currentDebugInfo().orElse(null));
    context.addInstruction(retain);
    ScopeInstr.register(variableName, scopeId, debugInfo);
  }


  /**
   * Get the IRGenerationContext for scope and debug management.
   * This allows helpers to access the stack-based context when needed.
   */
  public IRGenerationContext getContext() {
    return context;
  }

  /**
   * Get the original IRContext for legacy generator compatibility.
   * This provides access to parsed module, compiler flags, and other context
   * without parameter threading through the call stack.
   */
  public IRContext getIRContext() {
    return context.getCurrentIRContext();
  }

  public void returnVoid() {
    addInstruction(BranchInstr.returnVoid());
  }

  public void returnValue(String variableName, DebugInfo debugInfo) {
    addInstruction(BranchInstr.returnValue(variableName, debugInfo));
  }

  /**
   * Return a value using current context debug info.
   */
  public void returnValue(String variableName) {
    var debugInfo = context.currentDebugInfo().orElse(null);
    addInstruction(BranchInstr.returnValue(variableName, debugInfo));
  }

  /**
   * Add a raw instruction to the current context.
   */
  public void addInstruction(IRInstr instruction) {
    context.addInstruction(instruction);
  }

  /**
   * Add multiple raw instructions to the current context.
   */
  public void addInstructions(List<IRInstr> instructions) {
    context.addInstructions(instructions);
  }

  /**
   * Extract all built instructions and clear the builder.
   */
  public List<IRInstr> extractInstructions() {
    return context.extractInstructions();
  }
}