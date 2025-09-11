package org.ek9lang.compiler.phase7.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.ir.BasicBlockInstr;
import org.ek9lang.compiler.ir.BranchInstr;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.ControlFlowChainInstr;
import org.ek9lang.compiler.ir.DebugInfo;
import org.ek9lang.compiler.ir.GuardedAssignmentBlockInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.LabelInstr;
import org.ek9lang.compiler.ir.LiteralInstr;
import org.ek9lang.compiler.ir.LogicalOperationInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.QuestionOperatorInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.calls.CallDetailsBuilder;
import org.ek9lang.compiler.phase7.support.LogicalOperationContext;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
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
  private final VariableMemoryManagement memoryManagement;

  /**
   * Create a new instruction builder with stack-based context access.
   * This enables generators to access the current IRContext without parameter threading.
   */
  public IRInstructionBuilder(IRGenerationContext context) {
    this.context = context;
    this.memoryManagement = new VariableMemoryManagement();
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
   * Create a temporary variable name and register it with proper memory management.
   */
  public String createTempVariable() {
    var tempName = context.generateTempName();
    var variableDetails = context.createVariableDetails(tempName);
    //This is incorrect - memory management after creation and use.
    addMemoryManagement(variableDetails);
    return tempName;
  }

  /**
   * Create a temporary variable with custom scope.
   */
  public String createTempVariable(String scopeId) {
    var tempName = context.generateTempName();
    var variableDetails = context.createVariableDetails(tempName, scopeId);
    addMemoryManagement(variableDetails);
    return tempName;
  }

  /**
   * Add memory management instructions for a variable.
   */
  public void addMemoryManagement(VariableDetails variableDetails) {
    var instructions = memoryManagement.apply(ArrayList::new, variableDetails);
    context.addInstructions(instructions);
  }

  /**
   * Add memory management for variable name using current context.
   */
  public void addMemoryManagement(String variableName) {
    var variableDetails = context.createVariableDetails(variableName);
    addMemoryManagement(variableDetails);
  }

  /**
   * Create a basic block instruction using current context.
   */
  public BasicBlockInstr createBasicBlock(String labelPrefix) {
    var label = context.generateBlockLabel(labelPrefix);
    var instruction = new BasicBlockInstr(label);
    // Note: BasicBlockInstr is not added to instruction context as it's not an IRInstr
    return instruction;
  }

  /**
   * Create a basic block instruction with custom label.
   */
  public BasicBlockInstr createBasicBlock(String label, boolean useAsIs) {
    var instruction = new BasicBlockInstr(useAsIs ? label : context.generateBlockLabel(label));
    // Note: BasicBlockInstr is not added to instruction context as it's not an IRInstr
    return instruction;
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
   * Create a literal instruction using current context.
   */
  public LiteralInstr createLiteral(String variableName, String literalValue, String literalType) {
    var debugInfo = context.currentDebugInfo().orElse(null);
    var instruction = LiteralInstr.literal(variableName, literalValue, literalType, debugInfo);
    context.addInstruction(instruction);
    return instruction;
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
   * Create a call instruction with auto-generated result variable.
   */
  public CallInstr createCall(CallContext callContext, String resultType) {
    if (context.needsResultVariable(resultType)) {
      var resultVar = createTempVariable();
      // Set result variable in call context - this would need to be added to CallContext
      // For now, just create the call as-is
    }
    return createCall(callContext);
  }

  /**
   * Create a memory retain instruction using current context.
   */
  public MemoryInstr createMemoryRetain(String variableName) {
    var instruction = MemoryInstr.retain(variableName, context.currentDebugInfo().orElse(null));
    context.addInstruction(instruction);
    return instruction;
  }

  /**
   * Create a memory reference instruction using current context.
   */
  public MemoryInstr createMemoryReference(String variableName, String variableTypeName) {
    var instruction = MemoryInstr.reference(variableName, variableTypeName, context.currentDebugInfo().orElse(null));
    context.addInstruction(instruction);
    return instruction;
  }

  /**
   * Create a scope register instruction using current context.
   */
  public ScopeInstr createScopeRegister(String variableName) {
    var instruction = ScopeInstr.register(variableName, context.createBasicDetails());
    context.addInstruction(instruction);
    return instruction;
  }

  /**
   * Create a scope register instruction with custom scope.
   */
  public ScopeInstr createScopeRegister(String variableName, String scopeId) {
    var instruction = ScopeInstr.register(variableName, context.createBasicDetails(scopeId));
    context.addInstruction(instruction);
    return instruction;
  }

  /**
   * Create a branch instruction using current context.
   */
  public BranchInstr createBranch(String condition, String targetLabel) {
    // TODO: Fix constructor call once BranchInstr API is understood
    // var instruction = new BranchInstr(condition, targetLabel, context.createBasicDetails());
    // context.addInstruction(instruction);
    // return instruction;
    return null; // Placeholder
  }

  /**
   * Create a logical operation instruction using current context.
   */
  public LogicalOperationInstr createLogicalOperation(LogicalOperationContext logicalContext) {
    // TODO: Fix constructor call once LogicalOperationInstr API is understood
    // var instruction = new LogicalOperationInstr(logicalContext);
    // context.addInstruction(instruction);
    // return instruction;
    return null; // Placeholder
  }

  /**
   * Create a question operator instruction using current context.
   */
  public QuestionOperatorInstr createQuestionOperator(String variableName, String blockLabel) {
    // TODO: Fix constructor call once QuestionOperatorInstr API is understood
    // var variableDetails = context.createVariableDetails(variableName);
    // var instruction = new QuestionOperatorInstr(variableDetails, blockLabel);
    // context.addInstruction(instruction);
    // return instruction;
    return null; // Placeholder
  }

  /**
   * Create a control flow chain instruction using current context.
   */
  public ControlFlowChainInstr createControlFlowChain(List<Object> elements) {
    // TODO: Fix constructor call once ControlFlowChainInstr API is understood
    // var instruction = new ControlFlowChainInstr(elements, context.createBasicDetails());
    // context.addInstruction(instruction);
    // return instruction;
    return null; // Placeholder
  }

  /**
   * Create a guarded assignment block instruction using current context.
   */
  public GuardedAssignmentBlockInstr createGuardedAssignmentBlock(String blockLabel) {
    // TODO: Fix constructor call once GuardedAssignmentBlockInstr API is understood
    // var variableDetails = context.createVariableDetails(context.generateTempName());
    // var instruction = new GuardedAssignmentBlockInstr(variableDetails, blockLabel);
    // context.addInstruction(instruction);
    // return instruction;
    return null; // Placeholder
  }

  /**
   * Apply variable memory management pattern with instruction supplier.
   */
  public List<IRInstr> withMemoryManagement(Supplier<List<IRInstr>> instructionSupplier, 
                                            VariableDetails variableDetails) {
    return memoryManagement.apply(instructionSupplier, variableDetails);
  }

  /**
   * Apply variable memory management with auto-created variable.
   */
  public List<IRInstr> withMemoryManagement(Supplier<List<IRInstr>> instructionSupplier, 
                                            String variableName) {
    var variableDetails = context.createVariableDetails(variableName);
    return memoryManagement.apply(instructionSupplier, variableDetails);
  }

  /**
   * Get the associated IR generation context.
   */
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