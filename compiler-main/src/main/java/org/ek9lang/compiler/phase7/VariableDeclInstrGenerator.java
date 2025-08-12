package org.ek9lang.compiler.phase7;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for variable declarations.
 * Uses REFERENCE instructions for all variables - backend handles storage allocation.
 */
final class VariableDeclInstrGenerator extends AbstractVariableDeclGenerator
    implements BiFunction<EK9Parser.VariableDeclarationContext, String, List<IRInstr>> {

  VariableDeclInstrGenerator(final IRContext context) {
    super(context);
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
  }

  /**
   * Generate IR instructions for variable declaration with assignment.
   * Example: stdout &lt;- Stdout()
   * Generates: REFERENCE + assignment processing
   */
  public List<IRInstr> apply(final EK9Parser.VariableDeclarationContext ctx, final String scopeId) {
    AssertValue.checkNotNull("VariableDeclarationContext cannot be null", ctx);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    final var instructions = getDeclInstrs(ctx, scopeId);


    // Process the assignment expression (right-hand side)
    if (ctx.assignmentExpression() != null) {
      final var tempResult = context.generateTempName();
      final var assignmentExprInstrGenerator = new AssignmentExprInstrGenerator(context, scopeId);
      instructions.addAll(assignmentExprInstrGenerator.apply(ctx.assignmentExpression(), tempResult));

      final var varSymbol = context.getParsedModule().getRecordedSymbol(ctx);
      final var varName = varSymbol.getName();
      final var debugInfo = debugInfoCreator.apply(varSymbol);

      instructions.add(MemoryInstr.store(varName, tempResult, debugInfo));
      instructions.add(MemoryInstr.retain(varName, debugInfo));
    }

    return instructions;
  }
}