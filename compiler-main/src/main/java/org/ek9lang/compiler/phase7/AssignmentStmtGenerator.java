package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Process assignment statement: variable = expression
 * Uses RELEASE-then-RETAIN pattern for memory-safe assignments.
 * Handles assignments like someLocal = "Hi" and cross-scope assignments like rtn: claude.
 * For property fields, uses "this.fieldName" naming convention.
 * <p>
 * From the ANTLR grammar, we're looking at processing this.
 * </p>
 * <pre>
 *   assignmentStatement
 *     : (primaryReference | identifier | objectAccessExpression) op=(ASSIGN | ASSIGN2 | COLON |
 *     ASSIGN_UNSET | ADD_ASSIGN | SUB_ASSIGN | DIV_ASSIGN | MUL_ASSIGN | MERGE | REPLACE | COPY) assignmentExpression
 *     ;
 * </pre>
 */
final class AssignmentStmtGenerator extends AbstractGenerator implements
    BiFunction<EK9Parser.AssignmentStatementContext, String, List<IRInstr>> {

  AssignmentStmtGenerator(final IRContext context) {
    super(context);

  }

  @Override
  public List<IRInstr> apply(final EK9Parser.AssignmentStatementContext ctx, final String scopeId) {

    AssertValue.checkNotNull("Ctx cannot be null", ctx);
    AssertValue.checkNotNull("ScopeId cannot be null", scopeId);

    operationImplementedOrException(ctx.op);

    final var instructions = new ArrayList<IRInstr>();
    if (ctx.primaryReference() != null) {
      throw new CompilerException("PrimaryReference assignment not implemented");
    } else if (ctx.identifier() != null) {
      processIdentifierAssignment(ctx, scopeId, instructions);
    } else if (ctx.objectAccessExpression() != null) {
      throw new CompilerException("ObjectAccessExpression assignment not implemented");
    }

    return instructions;

  }

  private void operationImplementedOrException(final Token op) {

    if (isNotSimpleAssignment(op) && !isGuardedAssignment(op)) {
      throw new CompilerException("Operation " + op.getText() + " not yet implemented");
    }

  }

  private boolean isNotSimpleAssignment(final Token op) {
    return op.getType() != EK9Parser.ASSIGN
        && op.getType() != EK9Parser.ASSIGN2
        && op.getType() != EK9Parser.COLON;
  }

  private boolean isGuardedAssignment(final Token op) {
    return op.getType() == EK9Parser.ASSIGN_UNSET;
  }

  private boolean isMethodBasedAssignment(final Token op) {
    return isNotSimpleAssignment(op) && !isGuardedAssignment(op);
  }

  private void processIdentifierAssignment(final EK9Parser.AssignmentStatementContext ctx,
                                           final String scopeId, final List<IRInstr> instructions) {

    final var lhsSymbol = getRecordedSymbolOrException(ctx.identifier());

    final var generator = new AssignmentExprInstrGenerator(context, ctx.assignmentExpression(), scopeId);

    if (isMethodBasedAssignment(ctx.op)) {
      throw new CompilerException("Method Based Operation " + ctx.op.getText() + " not yet implemented");
    }
    final var assignExpressionToSymbol = new AssignExpressionToSymbol(context, true, generator, scopeId);

    if (isGuardedAssignment(ctx.op)) {

      final var guardedDetails = new GuardedAssignmentGenerator.GuardedAssignmentDetails(
          lhsSymbol, ctx.assignmentExpression(), scopeId);

      final var guardedGenerator = createGuardedGenerator(generator, assignExpressionToSymbol);
      instructions.addAll(guardedGenerator.apply(guardedDetails));
      return; // Early return since guarded assignment is complete
    }

    //So it is a simple assignment.
    instructions.addAll(assignExpressionToSymbol.apply(lhsSymbol, ctx.assignmentExpression()));

  }

  private GuardedAssignmentGenerator createGuardedGenerator(final AssignmentExprInstrGenerator generator,
                                                            final AssignExpressionToSymbol assignExpressionToSymbol) {

    final Function<ExprProcessingDetails, List<IRInstr>>
        expressionProcessor = details -> generator.apply(details.variableDetails().resultVariable());
    final var questionBlockGenerator = new QuestionBlockGenerator(context, expressionProcessor);
    return new GuardedAssignmentGenerator(context, questionBlockGenerator, assignExpressionToSymbol);

  }
}
