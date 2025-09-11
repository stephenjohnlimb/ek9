package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.BiFunction;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.phase7.generation.DebugInfoCreator;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for variable only declarations.
 * Uses REFERENCE instructions for all variables - backend handles storage allocation.
 * <p>
 * MIGRATED: Now uses IRInstructionBuilder with original IRContext access via stack infrastructure.
 * </p>
 */
public final class VariableOnlyDeclInstrGenerator
    implements BiFunction<EK9Parser.VariableOnlyDeclarationContext, String, List<IRInstr>> {

  private final IRInstructionBuilder instructionBuilder;
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();
  private final DebugInfoCreator debugInfoCreator;

  public VariableOnlyDeclInstrGenerator(final IRInstructionBuilder instructionBuilder) {
    this.instructionBuilder = instructionBuilder;
    // Access original IRContext via stack infrastructure - no parameter threading!
    this.debugInfoCreator = new DebugInfoCreator(instructionBuilder.getIRContext());
  }

  /**
   * Generate IR instructions for variable only declaration.
   * Example: someVar as String?
   * Generates: REFERENCE only (no assignment)
   */
  public List<IRInstr> apply(final EK9Parser.VariableOnlyDeclarationContext ctx,
                             final String scopeId) {
    AssertValue.checkNotNull("VariableOnlyDeclarationContext cannot be null", ctx);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    return getDeclInstrs(ctx, scopeId);
  }

  private List<IRInstr> getDeclInstrs(final ParseTree ctx, final String scopeId) {
    AssertValue.checkNotNull("Ctx cannot be null", ctx);
    AssertValue.checkNotNull("ScopeId cannot be null", scopeId);

    // Get symbol using original IRContext from stack infrastructure  
    final var variableSymbol = instructionBuilder.getIRContext().getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Symbol should be resolved by phases 1-6", variableSymbol);

    final var variableName = variableNameForIR.apply(variableSymbol);
    final var variableTypeName = typeNameOrException.apply(variableSymbol);

    // Extract debug info using original IRContext from stack infrastructure
    final var debugInfo = debugInfoCreator.apply(variableSymbol.getSourceToken());

    // Create memory reference instruction with proper debug info
    return List.of(MemoryInstr.reference(variableName, variableTypeName, debugInfo));
  }
}