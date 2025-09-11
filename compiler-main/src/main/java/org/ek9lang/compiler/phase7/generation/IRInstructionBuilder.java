package org.ek9lang.compiler.phase7.generation;

import java.util.List;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.LabelInstr;
import org.ek9lang.compiler.ir.instructions.LiteralInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.calls.CallDetailsBuilder;
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
   * Create a memory retain an register for memory management current context.
   */
  public void memoryRetainAndRegister(String variableName) {
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