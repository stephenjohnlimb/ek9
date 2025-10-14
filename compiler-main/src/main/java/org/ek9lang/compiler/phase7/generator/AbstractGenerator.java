package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.calls.CallDetailsForIsTrue;
import org.ek9lang.compiler.phase7.generation.DebugInfoCreator;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Acts as base for most generators as they all require the context and in most cases
 * need to generate debug information.
 * <p>
 * Uses stack-based IRGenerationContext for state management, eliminating parameter threading.
 * </p>
 */
abstract class AbstractGenerator {
  protected final IRGenerationContext stackContext;
  protected final DebugInfoCreator debugInfoCreator;
  protected final IRInstructionBuilder instructionBuilder;

  /**
   * Constructor accepting only IRGenerationContext - the single source of state.
   */
  AbstractGenerator(final IRGenerationContext stackContext) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", stackContext);
    this.stackContext = stackContext;
    this.debugInfoCreator = new DebugInfoCreator(stackContext.getCurrentIRContext());
    this.instructionBuilder = new IRInstructionBuilder(stackContext);
  }


  protected ISymbol getRecordedSymbolOrException(ParseTree node) {
    final var rtn = stackContext.getParsedModule().getRecordedSymbol(node);
    AssertValue.checkNotNull("Symbol should be resolved by phases 1-6",
        rtn);
    AssertValue.checkTrue("Symbol must have been given a type by phase 7",
        rtn.getType().isPresent());
    return rtn;
  }

  /**
   * Create a temporary variable with associated debug info.
   * This consolidates the common pattern of generating a temp name and wrapping it in VariableDetails.
   *
   * @param debugInfo The debug information for this variable
   * @return VariableDetails containing the temp variable name and debug info
   */
  protected VariableDetails createTempVariable(final DebugInfo debugInfo) {
    return new VariableDetails(stackContext.generateTempName(), debugInfo);
  }

  /**
   * Convert an EK9 Boolean to a primitive boolean by calling _true().
   * This consolidates the pattern used for backend optimization where primitive booleans
   * are more efficient than object booleans.
   *
   * @param booleanVar The EK9 Boolean variable to convert
   * @param debugInfo  Debug information for the conversion
   * @return List containing the conversion instruction and the primitive boolean variable name
   */
  protected PrimitiveBooleanConversion convertToPrimitiveBoolean(final String booleanVar,
                                                                 final DebugInfo debugInfo) {
    final var primitiveVar = stackContext.generateTempName();
    final var callDetailsForIsTrue = new CallDetailsForIsTrue();
    final var instruction = CallInstr.operator(
        new VariableDetails(primitiveVar, debugInfo),
        callDetailsForIsTrue.apply(booleanVar)
    );
    return new PrimitiveBooleanConversion(primitiveVar, instruction);
  }

  /**
   * Result of converting an EK9 Boolean to primitive boolean.
   * Contains both the primitive variable name and the conversion instruction.
   */
  protected record PrimitiveBooleanConversion(String primitiveVariable, IRInstr instruction) {
    /**
     * Add the conversion instruction to a list of instructions and return the primitive variable name.
     */
    public String addToInstructions(List<IRInstr> instructions) {
      instructions.add(instruction);
      return primitiveVariable;
    }
  }

}
