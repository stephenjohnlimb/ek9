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
 * Base class for all IR generators providing common utilities for safer symbol access,
 * temporary variable creation, and type extraction.
 * <p>
 * Uses stack-based IRGenerationContext for state management, eliminating parameter threading.
 * All generators inherit access to these safer utility methods to ensure consistency and
 * reduce boilerplate code across the IR generation phase.
 * </p>
 * <p>
 * <b>Core Principles:</b>
 * </p>
 * <ul>
 *   <li><b>Safer Symbol Access</b> - All symbol retrieval includes null and type checks</li>
 *   <li><b>Consistent Debug Info</b> - Debug info is always tied to source locations</li>
 *   <li><b>Reduced Boilerplate</b> - Common patterns are consolidated into helper methods</li>
 *   <li><b>Error Prevention</b> - Invariant violations throw CompilerException with context</li>
 * </ul>
 * <p>
 * <b>Available Safer Utilities:</b>
 * </p>
 * <ul>
 *   <li>{@link #getRecordedSymbolOrException(ParseTree)} - Get symbol with null/type validation</li>
 *   <li>{@link #createTempVariable(DebugInfo)} - Generate temp variable with debug info</li>
 *   <li>{@link #createTempVariableFromContext(ParseTree)} - Temp variable from parse tree context</li>
 *   <li>{@link #extractReturnType(ParseTree, ISymbol)} - Safe return type extraction from CallSymbol</li>
 * </ul>
 * <p>
 * <b>Usage Examples:</b>
 * </p>
 * <p>
 * <b>Pattern 1: Safe Symbol Access</b>
 * </p>
 * <pre>{@code
 * // DON'T: Manual symbol retrieval with potential null issues
 * final var symbol = stackContext.getParsedModule().getRecordedSymbol(ctx);
 * if (symbol == null) {
 *   throw new CompilerException("Symbol not found");
 * }
 *
 * // DO: Use safer utility with automatic validation
 * final var symbol = getRecordedSymbolOrException(ctx);
 * }</pre>
 * <p>
 * <b>Pattern 2: Temporary Variable Creation</b>
 * </p>
 * <pre>{@code
 * // DON'T: Manual temp variable creation (3 lines)
 * final var tempName = stackContext.generateTempName();
 * final var debugInfo = stackContext.createDebugInfo(ctx);
 * final var tempDetails = new VariableDetails(tempName, debugInfo);
 *
 * // DO: Use helper method (1 line when only VariableDetails needed)
 * final var tempDetails = createTempVariableFromContext(ctx);
 *
 * // If individual components are needed (e.g., for separate use):
 * final var tempDetails = createTempVariableFromContext(ctx);
 * final var tempName = tempDetails.resultVariable();
 * final var debugInfo = tempDetails.debugInfo();
 * }</pre>
 * <p>
 * <b>Pattern 3: Return Type Extraction</b>
 * </p>
 * <pre>{@code
 * // DON'T: Manual type extraction with multiple conditionals
 * final var exprSymbol = getRecordedSymbolOrException(ctx);
 * ISymbol returnType;
 * if (exprSymbol instanceof CallSymbol cs) {
 *   final var method = cs.getResolvedSymbolToCall();
 *   if (method instanceof MethodSymbol ms) {
 *     returnType = ms.getReturningSymbol().getType().orElse(fallback);
 *   } else if (method instanceof FunctionSymbol fs) {
 *     returnType = fs.getReturningSymbol().getType().orElse(fallback);
 *   } else {
 *     returnType = fallback;
 *   }
 * } else {
 *   returnType = exprSymbol.getType().orElseThrow();
 * }
 *
 * // DO: Use helper method
 * final var returnType = extractReturnType(ctx, fallbackSymbol);
 * }</pre>
 * <p>
 * <b>Integration with Other Safer Utilities:</b>
 * </p>
 * <p>
 * AbstractGenerator works in conjunction with other Phase 7 safer utilities:
 * </p>
 * <ul>
 *   <li><b>TypeNameOrException</b> - Extracts fully qualified type name from symbol</li>
 *   <li><b>SymbolTypeOrException</b> - Extracts type symbol with error handling</li>
 *   <li><b>OperationDetailContextOrError</b> - Locates operation context or throws</li>
 * </ul>
 * <p>
 * <b>When to Use Which Utility:</b>
 * </p>
 * <ul>
 *   <li>Need symbol from ParseTree? Use {@code getRecordedSymbolOrException(ctx)}</li>
 *   <li>Need type name as String? Use {@code typeNameOrException.apply(symbol)}</li>
 *   <li>Need type as ISymbol? Use {@code symbolTypeOrException.apply(symbol)}</li>
 *   <li>Need temp variable? Use {@code createTempVariableFromContext(ctx)}</li>
 *   <li>Need return type from call? Use {@code extractReturnType(ctx, fallback)}</li>
 * </ul>
 * <p>
 * <b>Design Philosophy:</b>
 * </p>
 * <p>
 * These utilities follow the "fail-fast" principle: if something is wrong (null symbol,
 * missing type, etc.), throw immediately with a descriptive error message rather than
 * propagating null values or invalid state. This makes debugging easier and prevents
 * cascading failures.
 * </p>
 *
 * @see org.ek9lang.compiler.common.TypeNameOrException
 * @see org.ek9lang.compiler.common.SymbolTypeOrException
 * @see org.ek9lang.compiler.phase7.support.OperationDetailContextOrError
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


  /**
   * Safely retrieve the recorded symbol for a parse tree node with automatic validation.
   * <p>
   * This is the primary method for accessing symbols during IR generation. It ensures:
   * </p>
   * <ul>
   *   <li>Symbol was resolved by phases 1-6 (not null)</li>
   *   <li>Symbol has an associated type (required for IR generation)</li>
   * </ul>
   * <p>
   * <b>When to use:</b> ALWAYS use this instead of manually calling
   * {@code stackContext.getParsedModule().getRecordedSymbol(node)} to ensure proper validation.
   * </p>
   *
   * @param node The parse tree node to retrieve the symbol for
   * @return The validated symbol with guaranteed type information
   * @throws IllegalArgumentException if symbol is null or has no type (compiler bug)
   */
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
   * <p>
   * This consolidates the common pattern of generating a temp name and wrapping it in VariableDetails.
   * Use this when you already have debug info available (e.g., passed as a method parameter).
   * </p>
   * <p>
   * <b>When to use:</b> When debug info is already available. If you have a ParseTree context,
   * use {@link #createTempVariableFromContext(ParseTree)} instead.
   * </p>
   * <p>
   * <b>Example:</b>
   * </p>
   * <pre>{@code
   * // When debug info is passed in as parameter
   * protected List<IRInstr> generateSomething(DebugInfo debugInfo) {
   *   final var tempDetails = createTempVariable(debugInfo);
   *   // ... use tempDetails
   * }
   * }</pre>
   *
   * @param debugInfo The debug information for this variable
   * @return VariableDetails containing the temp variable name and debug info
   */
  protected VariableDetails createTempVariable(final DebugInfo debugInfo) {
    return new VariableDetails(stackContext.generateTempName(), debugInfo);
  }

  /**
   * Create a temporary variable with debug info extracted from parse tree context.
   * <p>
   * This consolidates the pattern of: {@code createDebugInfo(ctx) → generateTempName() → new VariableDetails()}.
   * Preferred method when you have a ParseTree context available.
   * </p>
   * <p>
   * <b>When to use:</b> PREFERRED method when you have a ParseTree context (expression, statement, etc.).
   * Automatically extracts source position for accurate debugging.
   * </p>
   * <p>
   * <b>Example:</b>
   * </p>
   * <pre>{@code
   * // Processing list elements
   * for (final var exprCtx : listContext.expression()) {
   *   final var elementDetails = createTempVariableFromContext(exprCtx);
   *   // If you need the components separately:
   *   final var elementTemp = elementDetails.resultVariable();
   *   final var elementDebugInfo = elementDetails.debugInfo();
   * }
   * }</pre>
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
