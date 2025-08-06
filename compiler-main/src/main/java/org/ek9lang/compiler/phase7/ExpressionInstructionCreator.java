package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.CallInstruction;
import org.ek9lang.compiler.ir.IRInstruction;
import org.ek9lang.compiler.ir.LiteralInstruction;
import org.ek9lang.compiler.ir.MemoryInstruction;
import org.ek9lang.compiler.ir.ScopeInstruction;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.Symbol;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for expressions.
 * Generates new BasicBlock IR (IRInstructions) instead of old Block IR (INode).
 */
public final class ExpressionInstructionCreator {

  private final IRGenerationContext context;
  private final ObjectAccessInstructionCreator objectAccessCreator;
  private final DebugInfoCreator debugInfoCreator;

  public ExpressionInstructionCreator(final IRGenerationContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);

    this.context = context;
    this.objectAccessCreator = new ObjectAccessInstructionCreator(context);
    this.debugInfoCreator = new DebugInfoCreator(context);
  }

  /**
   * Generate IR instructions for expression.
   */
  public List<IRInstruction> apply(final EK9Parser.ExpressionContext ctx,
                                   final String resultVar,
                                   final String scopeId) {
    AssertValue.checkNotNull("ExpressionContext cannot be null", ctx);
    AssertValue.checkNotNull("resultVar cannot be null", resultVar);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    final var instructions = new ArrayList<IRInstruction>();

    // Handle direct calls (like Stdout() constructor calls)
    if (ctx.call() != null) {

      // Get the resolved symbol for the call
      final var callSymbol = context.getParsedModule().getRecordedSymbol(ctx.call());

      if (callSymbol instanceof CallSymbol resolvedCallSymbol) {
        final var toBeCalled = resolvedCallSymbol.getResolvedSymbolToCall();

        if (toBeCalled instanceof MethodSymbol methodSymbol && methodSymbol.isConstructor()) {
          // This is a constructor call
          final var parentScope = methodSymbol.getParentScope();
          final var typeName =
              (parentScope instanceof Symbol symbol) ? symbol.getFullyQualifiedName() : parentScope.toString();

          // Extract debug info if debugging instrumentation is enabled
          final var debugInfo = debugInfoCreator.apply(callSymbol);

          // Generate constructor call using actual resolved type name
          instructions.add(CallInstruction.constructor(resultVar, typeName, debugInfo));

          // Add memory management for LLVM targets (no-ops on JVM)
          instructions.add(MemoryInstruction.retain(resultVar, debugInfo));
          instructions.add(ScopeInstruction.register(resultVar, scopeId, debugInfo));
        }
      }
    } else if (ctx.objectAccessExpression() != null) {
      instructions.addAll(objectAccessCreator.apply(ctx.objectAccessExpression(), resultVar, scopeId));
    } else if (ctx.primary() != null) {
      // Handle primary expressions: literals, identifier references, parenthesized expressions
      instructions.addAll(processPrimaryExpression(ctx.primary(), resultVar, scopeId));
    }

    return instructions;
  }

  /**
   * Process primary expressions using symbol-driven approach.
   * Primary expressions include: literals, identifier references, parenthesized expressions.
   */
  private List<IRInstruction> processPrimaryExpression(final EK9Parser.PrimaryContext ctx,
                                                       final String resultVar,
                                                       final String scopeId) {
    final var instructions = new ArrayList<IRInstruction>();

    if (ctx.literal() != null) {
      // Handle literals: string, numeric, boolean, etc.
      instructions.addAll(processLiteral(ctx.literal(), resultVar, scopeId));
    } else if (ctx.identifierReference() != null) {
      // Handle identifier references: variable names
      instructions.addAll(processIdentifierReference(ctx.identifierReference(), resultVar, scopeId));
    } else if (ctx.expression() != null) {
      // Handle parenthesized expressions: (expression)
      instructions.addAll(apply(ctx.expression(), resultVar, scopeId));
    }
    // primaryReference (THIS, SUPER) would be handled here too if needed

    return instructions;
  }

  /**
   * Process literal expressions using resolved symbol information.
   * This ensures we get correct type information, including decorated names for generic types.
   */
  private List<IRInstruction> processLiteral(final EK9Parser.LiteralContext ctx,
                                             final String resultVar,
                                             final String scopeId) {
    final var instructions = new ArrayList<IRInstruction>();

    // Get the resolved symbol for this literal - phases 1-6 ensure all literals have resolved types
    final var literalSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Literal symbol should be resolved by phases 1-6", literalSymbol);

    // Get the type from the resolved symbol (could be decorated for generic contexts)
    final var literalType = literalSymbol.getType()
        .map(ISymbol::getFullyQualifiedName)
        .orElseThrow(() -> new RuntimeException("Literal should have resolved type by phase 7"));


    final var literalValue = literalSymbol.getName();

    // Extract debug info if debugging instrumentation is enabled
    final var debugInfo = debugInfoCreator.apply(literalSymbol);

    // Create literal instruction with resolved type information
    instructions.add(LiteralInstruction.literal(resultVar, literalValue, literalType, debugInfo));

    return instructions;
  }

  /**
   * Process identifier references using resolved symbol information.
   */
  private List<IRInstruction> processIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx,
                                                         final String resultVar,
                                                         final String scopeId) {
    final var instructions = new ArrayList<IRInstruction>();

    // Get the resolved symbol for this identifier - phases 1-6 ensure all identifiers are resolved
    final var identifierSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Identifier symbol should be resolved by phases 1-6", identifierSymbol);

    // Load the variable using its resolved name (could be decorated for generic contexts)
    final var variableName = identifierSymbol.getName();

    // Extract debug info if debugging instrumentation is enabled
    final var debugInfo = debugInfoCreator.apply(identifierSymbol);

    instructions.add(MemoryInstruction.load(resultVar, variableName, debugInfo));

    return instructions;
  }

}