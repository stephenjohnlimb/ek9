package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstruction;
import org.ek9lang.compiler.ir.MemoryInstruction;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for variable declarations.
 * Generates new BasicBlock IR (IRInstructions) instead of old Block IR (INode).
 */
public final class VariableDeclarationInstructionCreator {

  private final IRGenerationContext context;
  private final AssignmentExpressionInstructionCreator assignmentExpressionCreator;
  private final DebugInfoCreator debugInfoCreator;

  public VariableDeclarationInstructionCreator(final IRGenerationContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.assignmentExpressionCreator = new AssignmentExpressionInstructionCreator(context);
    this.debugInfoCreator = new DebugInfoCreator(context);
  }

  /**
   * Generate IR instructions for variable declaration with assignment.
   * Example: stdout &lt;- Stdout()
   */
  public List<IRInstruction> apply(final EK9Parser.VariableDeclarationContext ctx, final String scopeId) {
    AssertValue.checkNotNull("VariableDeclarationContext cannot be null", ctx);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    final var instructions = new ArrayList<IRInstruction>();

    // Get the resolved variable symbol
    final var varSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Variable symbol cannot be null", varSymbol);

    final var varName = varSymbol.getName();
    final var tempResult = context.generateTempName();

    // Process the assignment expression (right-hand side)
    if (ctx.assignmentExpression() != null) {
      instructions.addAll(assignmentExpressionCreator.apply(ctx.assignmentExpression(), tempResult, scopeId));

      // Store the result in the variable
      final var debugInfo = debugInfoCreator.apply(varSymbol);
      instructions.add(MemoryInstruction.store(varName, tempResult, debugInfo));
    }

    return instructions;
  }

  /**
   * Generate IR instructions for variable only declaration.
   * Example: var stdout Stdout
   */
  public List<IRInstruction> applyVariableOnly(final EK9Parser.VariableOnlyDeclarationContext ctx,
                                               final String scopeId) {
    AssertValue.checkNotNull("VariableOnlyDeclarationContext cannot be null", ctx);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    final var instructions = new ArrayList<IRInstruction>();

    // Get the resolved variable symbol
    final var varSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    AssertValue.checkNotNull("Variable symbol cannot be null", varSymbol);

    final var varName = varSymbol.getName();
    final var typeName = varSymbol.getType().orElse(varSymbol).getFullyQualifiedName();

    // Allocate space for the variable
    final var debugInfo = debugInfoCreator.apply(varSymbol);
    instructions.add(MemoryInstruction.alloca(varName, typeName, debugInfo));

    return instructions;
  }

}