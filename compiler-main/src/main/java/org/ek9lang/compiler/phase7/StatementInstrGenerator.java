package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.BranchInstr;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for statements.
 * Generates new BasicBlock IR (IRInstructions).
 */
final class StatementInstrGenerator {

  private final IRContext context;
  private final ObjectAccessInstrGenerator objectAccessCreator;
  private final ExpressionInstrGenerator expressionCreator;
  private final DebugInfoCreator debugInfoCreator;

  StatementInstrGenerator(final IRContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.objectAccessCreator = new ObjectAccessInstrGenerator(context);
    this.expressionCreator = new ExpressionInstrGenerator(context);
    this.debugInfoCreator = new DebugInfoCreator(context);
  }

  /**
   * Generate IR instructions for a statement.
   */
  public List<IRInstr> apply(final EK9Parser.StatementContext ctx, final String scopeId) {
    AssertValue.checkNotNull("StatementContext cannot be null", ctx);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    final var instructions = new ArrayList<IRInstr>();

    if (ctx.assertStatement() != null) {
      instructions.addAll(processAssertStatement(ctx.assertStatement(), scopeId));
    } else if (ctx.assignmentStatement() != null) {
      instructions.addAll(processAssignmentStatement(ctx.assignmentStatement(), scopeId));
    } else if (ctx.objectAccessExpression() != null) {
      final var tempResult = context.generateTempName();
      instructions.addAll(objectAccessCreator.apply(ctx.objectAccessExpression(), tempResult, scopeId));
    }

    return instructions;
  }

  /**
   * Process assert statement: ASSERT expression
   * Uses EK9 Boolean._true() method to get primitive boolean for assertion.
   */
  private List<IRInstr> processAssertStatement(final EK9Parser.AssertStatementContext ctx, final String scopeId) {

    // Evaluate the assert expression 
    final var tempExprResult = context.generateTempName();
    final var instructions = new ArrayList<>(expressionCreator.apply(ctx.expression(), tempExprResult, scopeId));

    // Call the _true() method to get primitive boolean (true if set AND true)
    final var tempBoolResult = context.generateTempName();
    final var exprSymbol = context.getParsedModule().getRecordedSymbol(ctx.expression());
    final var debugInfo = debugInfoCreator.apply(exprSymbol);

    // Create method call with type information (_true() method on Boolean)
    final var callDetails = new CallDetails(tempExprResult,
        "org.ek9.lang::Boolean", "_true",
        List.of(), "boolean", List.of());

    instructions.add(CallInstr.call(tempBoolResult, debugInfo, callDetails));

    // Assert on the primitive boolean result  
    instructions.add(BranchInstr.assertValue(tempBoolResult, debugInfo));

    return instructions;
  }

  /**
   * Process assignment statement: variable = expression
   * Uses RELEASE-then-RETAIN pattern for memory-safe assignments.
   * Handles assignments like someLocal = "Hi" and cross-scope assignments like rtn: claude.
   */
  private List<IRInstr> processAssignmentStatement(final EK9Parser.AssignmentStatementContext ctx,
                                                   final String scopeId) {
    final var instructions = new ArrayList<IRInstr>();

    // Get the target variable (left side of assignment)
    String targetVariable = null;
    if (ctx.identifier() != null) {
      targetVariable = ctx.identifier().getText();
    }

    if (targetVariable != null) {
      final var debugInfo = debugInfoCreator.apply(context.getParsedModule().getRecordedSymbol(ctx));

      // RELEASE: Decrement reference count of current target value (tolerant of uninitialized)
      instructions.add(MemoryInstr.release(targetVariable, debugInfo));

      // Evaluate the assignment expression (right side)
      final var tempResult = context.generateTempName();
      instructions.addAll(expressionCreator.apply(ctx.assignmentExpression().expression(), tempResult, scopeId));

      // RETAIN: Increment reference count of new value to keep it alive
      instructions.add(MemoryInstr.retain(tempResult, debugInfo));

      // STORE: Assign new value to target variable
      instructions.add(MemoryInstr.store(targetVariable, tempResult, debugInfo));
    }

    return instructions;
  }
}