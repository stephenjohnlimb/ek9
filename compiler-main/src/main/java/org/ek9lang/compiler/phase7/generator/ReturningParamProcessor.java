package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.ReturnVariableDetails;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Processes returningParam context from expression forms of control flow constructs.
 * <p>
 * Expression forms like {@code result &lt;- switch expr} have a returningParam that declares
 * the return variable. This processor extracts the variable information and generates
 * the appropriate setup instructions.
 * </p>
 * The setup instructions include REFERENCE declaration, initial value assignment,
 * RETAIN, and SCOPE_REGISTER in the outer scope. This ensures proper ARC ownership
 * when branch results are assigned to the return variable.
 *
 * @see ReturnVariableDetails
 * @see ExpressionResultAssigner
 */
public final class ReturningParamProcessor extends AbstractGenerator {

  private final GeneratorSet generators;

  public ReturningParamProcessor(final IRGenerationContext stackContext,
                                 final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
  }

  /**
   * Process a returningParam context and extract return variable details.
   * <p>
   * The returningParam grammar is:
   * </p>
   * <pre>
   * returningParam
   *   : directive? LEFT_ARROW NL+ INDENT directive? NL* (variableDeclaration | variableOnlyDeclaration) NL DEDENT NL+
   *   | directive? LEFT_ARROW (variableDeclaration | variableOnlyDeclaration) NL+
   *   ;
   * </pre>
   *
   * @param ctx      The returningParam context (can be null for statement forms)
   * @param scopeId  The outer scope ID where the return variable should be registered
   * @return ReturnVariableDetails with setup instructions, or ReturnVariableDetails.none() if ctx is null
   */
  public ReturnVariableDetails process(final EK9Parser.ReturningParamContext ctx,
                                       final String scopeId) {
    if (ctx == null) {
      return ReturnVariableDetails.none();
    }

    final var setupInstructions = new ArrayList<IRInstr>();

    // Process return variable declaration
    if (ctx.variableDeclaration() != null) {
      // Return variable with initialization: <- rtn as String: String()
      setupInstructions.addAll(generators.variableDeclGenerator.apply(ctx.variableDeclaration()));
      return createReturnDetails(ctx.variableDeclaration(), setupInstructions);
    } else if (ctx.variableOnlyDeclaration() != null) {
      // Return variable without initialization: <- rtn as String?
      setupInstructions.addAll(generators.variableOnlyDeclGenerator.apply(ctx.variableOnlyDeclaration()));
      return createReturnDetails(ctx.variableOnlyDeclaration(), setupInstructions);
    }

    // Should not reach here - grammar ensures one of the above
    return ReturnVariableDetails.none();
  }

  /**
   * Check if a returningParam context represents an expression form.
   *
   * @param ctx The returningParam context (can be null)
   * @return true if this is an expression form (ctx is not null)
   */
  public boolean isExpressionForm(final EK9Parser.ReturningParamContext ctx) {
    return ctx != null;
  }

  /**
   * Extract the return variable symbol from a returningParam context.
   *
   * @param ctx The returningParam context
   * @return The symbol for the return variable
   */
  public ISymbol getReturnSymbol(final EK9Parser.ReturningParamContext ctx) {
    AssertValue.checkNotNull("returningParam context cannot be null", ctx);

    if (ctx.variableDeclaration() != null) {
      return getRecordedSymbolOrException(ctx.variableDeclaration());
    } else if (ctx.variableOnlyDeclaration() != null) {
      return getRecordedSymbolOrException(ctx.variableOnlyDeclaration());
    }

    throw new CompilerException(
        "Invalid returningParam - expected variableDeclaration or variableOnlyDeclaration");
  }

  /**
   * Get the return variable name in IR format.
   *
   * @param ctx The returningParam context
   * @return The variable name for use in IR
   */
  public String getReturnVariableName(final EK9Parser.ReturningParamContext ctx) {
    return new VariableNameForIR().apply(getReturnSymbol(ctx));
  }

  /**
   * Get the return variable type name in EK9 format.
   *
   * @param ctx The returningParam context
   * @return The type name (e.g., "org.ek9.lang::String")
   */
  public String getReturnVariableType(final EK9Parser.ReturningParamContext ctx) {
    return typeNameOrException.apply(getReturnSymbol(ctx));
  }

  /**
   * Create debug info from the returningParam context.
   *
   * @param ctx The returningParam context
   * @return DebugInfo for IR instructions
   */
  public DebugInfo createDebugInfo(final EK9Parser.ReturningParamContext ctx) {
    return stackContext.createDebugInfo(ctx);
  }

  private ReturnVariableDetails createReturnDetails(
      final EK9Parser.VariableDeclarationContext ctx,
      final List<IRInstr> setupInstructions) {
    final var symbol = getRecordedSymbolOrException(ctx);
    final var variableName = new VariableNameForIR().apply(symbol);
    final var typeName = typeNameOrException.apply(symbol);
    return ReturnVariableDetails.withSetup(variableName, typeName, setupInstructions);
  }

  private ReturnVariableDetails createReturnDetails(
      final EK9Parser.VariableOnlyDeclarationContext ctx,
      final List<IRInstr> setupInstructions) {
    final var symbol = getRecordedSymbolOrException(ctx);
    final var variableName = new VariableNameForIR().apply(symbol);
    final var typeName = typeNameOrException.apply(symbol);
    return ReturnVariableDetails.withSetup(variableName, typeName, setupInstructions);
  }
}
