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
import org.ek9lang.core.CompilerException;

/**
 * Creates IR instructions for statements.
 * Generates new BasicBlock IR (IRInstructions).
 * <p>From the ANTLR grammar this has to support the following:</p>
 * <pre>
 *   statement
 *     : ifStatement
 *     | assertStatement
 *     | assignmentStatement
 *     | identifierReference op=(INC | DEC)
 *     | call
 *     | throwStatement
 *     | objectAccessExpression
 *     | switchStatementExpression
 *     | tryStatementExpression
 *     | whileStatementExpression
 *     | forStatementExpression
 *     | streamStatement
 *     ;
 * </pre>
 * TODO put in full if/else with exceptions for not implemented yet.
 * Also TODO pull out the methods to separate functions as this will get too large otherwise.
 */
final class StmtInstrGenerator {

  private final IRContext context;
  private final ObjectAccessInstrGenerator objectAccessCreator;
  private final ExprInstrGenerator expressionCreator;
  private final DebugInfoCreator debugInfoCreator;

  StmtInstrGenerator(final IRContext context) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", context);
    this.context = context;
    this.objectAccessCreator = new ObjectAccessInstrGenerator(context);
    this.expressionCreator = new ExprInstrGenerator(context);
    this.debugInfoCreator = new DebugInfoCreator(context);
  }

  /**
   * Generate IR instructions for a statement.
   */
  public List<IRInstr> apply(final EK9Parser.StatementContext ctx, final String scopeId) {
    AssertValue.checkNotNull("StatementContext cannot be null", ctx);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);

    final var instructions = new ArrayList<IRInstr>();

    if (ctx.ifStatement() != null) {
      throw new CompilerException("If not implemented");
    } else if (ctx.assertStatement() != null) {
      processAssertStatement(ctx.assertStatement(), scopeId, instructions);
    } else if (ctx.assignmentStatement() != null) {
      processAssignmentStatement(ctx.assignmentStatement(), scopeId, instructions);
    } else if (ctx.identifierReference() != null) {
      throw new CompilerException("Identifier inc/dec not implemented");
    } else if (ctx.call() != null) {
      throw new CompilerException("Call not implemented");
    } else if (ctx.throwStatement() != null) {
      throw new CompilerException("Throw not implemented");
    } else if (ctx.objectAccessExpression() != null) {
      processObjectAccessExpression(ctx.objectAccessExpression(), scopeId, instructions);
    } else if (ctx.switchStatementExpression() != null) {
      throw new CompilerException("Switch not implemented");
    } else if (ctx.tryStatementExpression() != null) {
      throw new CompilerException("Try not implemented");
    } else if (ctx.whileStatementExpression() != null) {
      throw new CompilerException("While not implemented");
    } else if (ctx.forStatementExpression() != null) {
      throw new CompilerException("For not implemented");
    } else if (ctx.streamStatement() != null) {
      throw new CompilerException("Stream not implemented");
    } else {
      throw new CompilerException("Unexpected condition");
    }

    return instructions;
  }

  private void processObjectAccessExpression(final EK9Parser.ObjectAccessExpressionContext ctx,
                                             final String scopeId, final List<IRInstr> instructions) {
    final var tempResult = context.generateTempName();
    instructions.addAll(objectAccessCreator.apply(ctx, tempResult, scopeId));
  }

  /**
   * Process assert statement: ASSERT expression
   * Uses EK9 Boolean._true() method to get primitive boolean for assertion.
   */
  private void processAssertStatement(final EK9Parser.AssertStatementContext ctx,
                                      final String scopeId, final List<IRInstr> instructions) {

    // Evaluate the assert expression 
    final var tempExprResult = context.generateTempName();
    instructions.addAll(expressionCreator.apply(ctx.expression(), tempExprResult, scopeId));

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

  }

  /**
   * Process assignment statement: variable = expression
   * Uses RELEASE-then-RETAIN pattern for memory-safe assignments.
   * Handles assignments like someLocal = "Hi" and cross-scope assignments like rtn: claude.
   * For property fields, uses "this.fieldName" naming convention.
   */
  private void processAssignmentStatement(final EK9Parser.AssignmentStatementContext ctx,
                                          final String scopeId, final List<IRInstr> instructions) {

    // Get the target variable (left side of assignment)
    String targetVariable = null;
    if (ctx.identifier() != null) {
      final var identifierName = ctx.identifier().getText();

      // Check if this is a property field assignment by looking up the symbol
      final var symbol = context.getParsedModule().getRecordedSymbol(ctx.identifier());
      if (symbol instanceof org.ek9lang.compiler.symbols.VariableSymbol varSymbol && varSymbol.isPropertyField()) {
        // Use "this.fieldName" for property fields
        targetVariable = "this." + identifierName;
      } else {
        // Use regular variable name (potentially with scope qualification later)
        targetVariable = identifierName;
      }
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

  }
}