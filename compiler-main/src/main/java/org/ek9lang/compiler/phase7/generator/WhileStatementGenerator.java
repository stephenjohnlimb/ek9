package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Generates IR for while loops using CONTROL_FLOW_CHAIN.
 * Currently handles simple while loops (no guards, statement form only).
 * <p>
 * Scope structure (matches if/else pattern exactly):
 * </p>
 * <pre>
 *   Outer Scope (_scope_1): Loop wrapper for future guards
 *   Condition Scope (_scope_2): Condition temps accumulate here (across iterations)
 *   Iteration Scope (_scope_3): Body execution, enters/exits each iteration
 * </pre>
 * <p>
 * The two-scope pattern before CONTROL_FLOW_CHAIN is mandatory architectural infrastructure
 * that enables future guard and expression form support without refactoring.
 * </p>
 */
public final class WhileStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.WhileStatementExpressionContext, List<IRInstr>> {

  private final GeneratorSet generators;

  public WhileStatementGenerator(final IRGenerationContext stackContext,
                                 final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.WhileStatementExpressionContext ctx) {
    AssertValue.checkNotNull("WhileStatementExpressionContext cannot be null", ctx);

    // Detect which form: while ... or do ... while
    if (ctx.DO() != null) {
      throw new CompilerException("Do-while loops not yet implemented");
    }

    // Check for expression form (returningParam)
    if (ctx.returningParam() != null) {
      throw new CompilerException("While loop expression form not yet implemented");
    }

    // Simple while loop (statement form)
    return generateSimpleWhileLoop(ctx);
  }

  /**
   * Generate IR for simple while loop: while condition { body }
   * Follows the same two-scope pattern as if/else for architectural consistency.
   */
  private List<IRInstr> generateSimpleWhileLoop(
      final EK9Parser.WhileStatementExpressionContext ctx) {

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Check for guard (preFlowStatement)
    if (ctx.preFlowStatement() != null) {
      throw new CompilerException("While loop guards not yet implemented");
    }

    // SCOPE 1: Enter loop outer scope (for future guards)
    // This matches if/else pattern exactly - outer wrapper for guards
    final var outerScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(outerScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(outerScopeId, debugInfo));

    // SCOPE 2: Enter condition scope (condition temps register here)
    // This matches if/else pattern exactly - shared condition scope
    final var conditionScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(conditionScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(conditionScopeId, debugInfo));

    // Process condition expression (temps registered to condition scope)
    // Unlike if/else where this executes once, backend will loop back here
    final var conditionResult = createTempVariable(debugInfo);
    final var conditionEvaluation = new ArrayList<>(
        generators.exprGenerator.apply(
            new ExprProcessingDetails(ctx.control, conditionResult)
        )
    );

    // Add primitive boolean conversion for backend branching
    final var conversion = convertToPrimitiveBoolean(
        conditionResult.resultVariable(), debugInfo);
    final var primitiveCondition = conversion.addToInstructions(conditionEvaluation);

    // Enter iteration scope for body
    // This scope enters/exits each iteration
    final var iterationScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(iterationScopeId, debugInfo, IRFrameType.BLOCK);

    // Process body with scope management
    final var bodyEvaluation = new ArrayList<IRInstr>();
    bodyEvaluation.add(ScopeInstr.enter(iterationScopeId, debugInfo));
    bodyEvaluation.addAll(processBlockStatements(ctx.instructionBlock()));
    bodyEvaluation.add(ScopeInstr.exit(iterationScopeId, debugInfo));

    // Exit iteration scope from context
    stackContext.exitScope();

    // Create ConditionCaseDetails (single case for while loop)
    final var conditionCase = ConditionCaseDetails.createExpression(
        iterationScopeId,                     // case_scope_id (iteration scope)
        conditionEvaluation,                  // condition instructions
        conditionResult.resultVariable(),     // EK9 Boolean result
        primitiveCondition,                   // primitive boolean for branching
        bodyEvaluation,                       // body with SCOPE_ENTER/EXIT
        null                                  // no result (statement form)
    );

    // Create CONTROL_FLOW_CHAIN with condition scope ID
    // Backend will interpret WHILE_LOOP to generate loop-back logic
    final var whileDetails = ControlFlowChainDetails.createWhileLoop(
        List.of(conditionCase),
        debugInfo,
        conditionScopeId    // Condition scope ID (NOT outer scope)
    );

    instructions.addAll(generators.controlFlowChainGenerator.apply(whileDetails));

    // Exit condition scope
    instructions.add(ScopeInstr.exit(conditionScopeId, debugInfo));
    stackContext.exitScope();

    // Exit outer scope
    instructions.add(ScopeInstr.exit(outerScopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Process all block statements in an instruction block.
   * Consolidates the common pattern of iterating through block statements.
   */
  private List<IRInstr> processBlockStatements(
      final EK9Parser.InstructionBlockContext ctx) {
    final var instructions = new ArrayList<IRInstr>();
    for (var blockStatement : ctx.blockStatement()) {
      instructions.addAll(generators.blockStmtGenerator.apply(blockStatement));
    }
    return instructions;
  }
}
