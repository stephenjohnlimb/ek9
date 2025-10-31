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
 * 1. throw Exception("message") - constructor call expression (TODO: not yet implemented)
 * 2. throw exceptionVariable - variable reference (implemented)
 * </p>
 * <p>
 * Key behaviors:
 * - For identifier: Applies RETAIN (ownership transfer) then THROW
 * - THROW is a terminating instruction (transfers control to exception mechanism)
 * - SCOPE_EXIT after throw is unreachable in normal flow (backend executes during unwinding)
 * </p>
 * <p>
 * ARC Ownership Transfer Semantics:
 * Following the Producer/Consumer pattern from EK9_ARC_OWNERSHIP_TRANSFER_PATTERN.md:
 * <pre>
 * Declaration (normal variable management):
 *   RETAIN + SCOPE_REGISTER    // Variable managed in declaration scope
 *
 * Throw (Producer - transfers ownership):
 *   RETAIN (no SCOPE_REGISTER) // Increment for transfer
 *   THROW                      // Transfer control
 *
 * Stack Unwinding (backend responsibility):
 *   SCOPE_EXIT                 // Backend releases declaration scope reference
 *
 * Catch (Consumer - receives ownership):
 *   REFERENCE (no RETAIN)      // Receive exception
 *   SCOPE_REGISTER             // Take ownership for catch scope
 * </pre>
 * Result: Exception has refcount = 1 in catch handler, gets released on catch scope exit.
 * </p>
 */
public final class ThrowStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.ThrowStatementContext, List<IRInstr>> {

  /**
   * Constructor accepting injected GeneratorSet for access to sub-generators.
   */
  ThrowStatementGenerator(final IRGenerationContext stackContext,
                          final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.ThrowStatementContext ctx) {
    AssertValue.checkNotNull("ThrowStatementContext cannot be null", ctx);

    final var debugInfo = stackContext.createDebugInfo(ctx.THROW().getSymbol());

    // Determine which form: call or identifierReference
    if (ctx.call() != null) {
      // Form 1: throw Exception("message") - constructor call
      // TODO: Implement when CallInstrGenerator properly handles constructor calls
      throw new CompilerException("throw with constructor call not yet implemented - use variable form");
    } else if (ctx.identifierReference() != null) {
      // Form 2: throw exceptionVariable - variable reference
      return new ArrayList<>(processThrowWithIdentifier(ctx.identifierReference(), debugInfo));
    }
    throw new CompilerException("throw statement must have call or identifierReference");


  }

  /**
   * Process throw with identifier reference (variable form).
   * Implements ARC ownership transfer pattern for exception throwing.
   * <p>
   * Pattern: Producer (throw) must RETAIN without SCOPE_REGISTER.
   * This ensures correct refcount during stack unwinding:
   * - Declaration scope has RETAIN + SCOPE_REGISTER (will release during unwinding)
   * - THROW adds additional RETAIN (for ownership transfer)
   * - Stack unwinding releases declaration scope's reference
   * - Catch receives exception with refcount = 1 (no additional RETAIN needed)
   * </p>
   *
   * @param identCtx  The identifier reference context (variable name)
   * @param debugInfo Debug information for throw statement
   * @return List of IR instructions
   */
  private List<IRInstr> processThrowWithIdentifier(
      final EK9Parser.IdentifierReferenceContext identCtx,
      final org.ek9lang.compiler.ir.support.DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();

    // Get the variable name from identifier context
    final var exceptionVarName = identCtx.getText();

    // ARC OWNERSHIP TRANSFER PATTERN:
    // Add RETAIN (but NOT SCOPE_REGISTER) to increment refcount for transfer.
    // This balances the release that will occur during stack unwinding.
    // The exception object was already RETAINED + SCOPE_REGISTERED at declaration.
    // During unwinding, backend executes SCOPE_EXIT which releases that reference.
    // This additional RETAIN ensures the exception survives unwinding with refcount = 1.
    instructions.add(org.ek9lang.compiler.ir.instructions.MemoryInstr.retain(exceptionVarName, debugInfo));

    // Transfer ownership to exception mechanism
    instructions.add(ThrowInstr.throwException(exceptionVarName, debugInfo));

    return instructions;
  }
}
