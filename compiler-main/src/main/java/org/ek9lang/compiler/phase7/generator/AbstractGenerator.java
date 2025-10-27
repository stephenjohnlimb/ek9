package org.ek9lang.compiler.phase7.generator;

import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.ir.support.DebugInfo;
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
   * Create a temporary variable with debug info extracted from parse tree context.
   * This consolidates the pattern of: createDebugInfo(ctx) → createTempVariable(debugInfo).
   * Used for accurate position tracking of operands in binary/unary operations.
   *
   * @param ctx The parse tree context to extract position from
   * @return VariableDetails containing the temp variable name and context-specific debug info
   */
  protected VariableDetails createTempVariableFromContext(final ParseTree ctx) {
    final var debugInfo = stackContext.createDebugInfo(ctx);
    return createTempVariable(debugInfo);
  }

  /**
   * Extract return type from a CallSymbol's resolved method.
   * Handles the pattern: CallSymbol → MethodSymbol/FunctionSymbol → returning type.
   * Falls back to expression type if not a CallSymbol.
   *
   * @param ctx The parse tree context containing the expression symbol
   * @param fallbackSymbol Symbol to use if resolved method has no return type
   * @return The return type of the operation
   */
  protected ISymbol extractReturnType(final ParseTree ctx, final ISymbol fallbackSymbol) {
    final var exprSymbol = getRecordedSymbolOrException(ctx);

    if (exprSymbol instanceof org.ek9lang.compiler.symbols.CallSymbol cs) {
      final var resolvedMethod = cs.getResolvedSymbolToCall();
      return switch (resolvedMethod) {
        case org.ek9lang.compiler.symbols.MethodSymbol ms ->
            ms.getReturningSymbol().getType().orElse(fallbackSymbol);
        case org.ek9lang.compiler.symbols.FunctionSymbol fs ->
            fs.getReturningSymbol().getType().orElse(fallbackSymbol);
        default -> fallbackSymbol;  // Fallback for other symbol types
      };
    }

    return exprSymbol.getType().orElseThrow();
  }

}
