package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for assignment expressions.
 * Generates new BasicBlock IR (IRInstructions).
 * <p>
 *   Note that this is just really a 'pointer' assignment to some existing allocated object/memory.
 *   It is not a deep copy in any way.
 * </p>
 */
final class AssignmentExpressionInstrGenerator {

  private final ExpressionInstrGenerator expressionCreator;
  private final String scopeId;

  AssignmentExpressionInstrGenerator(final IRContext context, final String scopeId) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    this.expressionCreator = new ExpressionInstrGenerator(context);
    this.scopeId = scopeId;
  }

  /**
   * Generate IR instructions for assignment expression.
   */
  public List<IRInstr> apply(final EK9Parser.AssignmentExpressionContext ctx,
                             final String resultVar) {

    AssertValue.checkNotNull("AssignmentExpressionContext cannot be null", ctx);
    AssertValue.checkNotNull("resultVar cannot be null", resultVar);
    
    final var instructions = new ArrayList<IRInstr>();
    
    // Navigate through the assignment expression to find the actual expression
    if (ctx.expression() != null) {
      instructions.addAll(expressionCreator.apply(ctx.expression(), resultVar, scopeId));
    }
    
    return instructions;
  }
}