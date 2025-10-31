package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.ThrowInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Generates IR for throw statements.
 * <p>
 * Grammar support:
 * throwStatement: THROW (call | identifierReference)
 * </p>
 * <p>
 * Two forms:
 * 1. throw Exception("message") - constructor call expression
 * 2. throw exceptionVariable - variable reference
 * </p>
 * <p>
 * Key behaviors:
 * - For call: Uses callGenerator to create exception object with ARC
 * - For identifier: Loads variable and applies RETAIN + SCOPE_REGISTER
 * - Generates THROW instruction with exception variable
 * - THROW is a terminating instruction (no code follows in same basic block)
 * - SCOPE_EXIT after throw is unreachable (backend handles stack unwinding)
 * </p>
 * <p>
 * ARC Semantics:
 * - Exception object created/loaded with standard RETAIN + SCOPE_REGISTER
 * - THROW transfers ownership to exception mechanism
 * - Backend maintains refcount during stack unwinding
 * - CATCH receives ownership without additional RETAIN (clean transfer)
 * </p>
 */
public final class ThrowStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.ThrowStatementContext, List<IRInstr>> {

  private final GeneratorSet generators;

  /**
   * Constructor accepting injected GeneratorSet for access to sub-generators.
   */
  ThrowStatementGenerator(final IRGenerationContext stackContext,
                          final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.ThrowStatementContext ctx) {
    AssertValue.checkNotNull("ThrowStatementContext cannot be null", ctx);

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = stackContext.createDebugInfo(ctx.THROW().getSymbol());

    // Determine which form: call or identifierReference
    if (ctx.call() != null) {
      // Form 1: throw Exception("message") - constructor call
      // TODO: Implement when CallInstrGenerator properly handles constructor calls
      throw new CompilerException("throw with constructor call not yet implemented - use variable form");
    } else if (ctx.identifierReference() != null) {
      // Form 2: throw exceptionVariable - variable reference
      instructions.addAll(processThrowWithIdentifier(ctx.identifierReference(), debugInfo));
    } else {
      throw new CompilerException("throw statement must have call or identifierReference");
    }

    return instructions;
  }

  /**
   * Process throw with identifier reference (variable form).
   * Throws existing exception variable directly.
   * The variable already has proper ARC management from its declaration.
   *
   * @param identCtx The identifier reference context (variable name)
   * @param debugInfo Debug information for throw statement
   * @return List of IR instructions
   */
  private List<IRInstr> processThrowWithIdentifier(
      final EK9Parser.IdentifierReferenceContext identCtx,
      final org.ek9lang.compiler.ir.support.DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();

    // Get the variable name directly from the identifier context
    // The variable already exists and has been RETAINED/SCOPE_REGISTERED at declaration
    final var exceptionVarName = identCtx.getText();

    // Throw the exception - transfers ownership to exception mechanism
    // No need to load/retain again - variable is already managed
    instructions.add(ThrowInstr.throwException(exceptionVarName, debugInfo));

    return instructions;
  }
}
