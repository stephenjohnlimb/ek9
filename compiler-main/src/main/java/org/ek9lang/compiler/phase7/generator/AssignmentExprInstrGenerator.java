package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.core.AssertValue;

/**
 * Creates IR instructions for assignment expressions.
 * Generates new BasicBlock IR (IRInstructions).
 * <p>
 * Note that this is just really a 'pointer' assignment to some existing allocated object/memory.
 * It is not a deep copy in any way.
 * </p>
 * <p>
 * THis deals with the following ANTLR grammar.
 * </p>
 * <pre>
 *   assignmentExpression
 *     : expression
 *     | guardExpression
 *     | dynamicClassDeclaration
 *     | switchStatementExpression
 *     | tryStatementExpression
 *     | whileStatementExpression
 *     | forStatementExpression
 *     | streamExpression
 *     ;
 * </pre>
 */
final class AssignmentExprInstrGenerator extends AbstractGenerator
    implements Function<String, List<IRInstr>> {

  private final GeneratorSet generators;
  private final EK9Parser.AssignmentExpressionContext ctx;

  /**
   * Constructor accepting injected GeneratorSet.
   * Provides access to all generators including control flow expression generators.
   */
  AssignmentExprInstrGenerator(final IRGenerationContext stackContext,
                               final GeneratorSet generators,
                               final EK9Parser.AssignmentExpressionContext ctx) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    AssertValue.checkNotNull("AssignmentExpressionContext cannot be null", ctx);

    this.generators = generators;
    this.ctx = ctx;
  }

  /**
   * Generate IR instructions for assignment expression using stack-based scope management.
   * STACK-BASED: Gets scope ID from stack context instead of constructor parameter.
   */
  public List<IRInstr> apply(final String rhsExprResult) {


    AssertValue.checkNotNull("RhsExprResult cannot be null", rhsExprResult);

    if (ctx.expression() != null) {
      return processExpression(rhsExprResult);
    } else if (ctx.guardExpression() != null) {
      AssertValue.fail("guardExpression not implemented");
    } else if (ctx.dynamicClassDeclaration() != null) {
      AssertValue.fail("dynamicClassDeclaration not implemented");
    } else if (ctx.switchStatementExpression() != null) {
      return processSwitchExpression(rhsExprResult);
    } else if (ctx.tryStatementExpression() != null) {
      return processTryExpression(rhsExprResult);
    } else if (ctx.whileStatementExpression() != null) {
      return processWhileExpression(rhsExprResult);
    } else if (ctx.forStatementExpression() != null) {
      return processForExpression(rhsExprResult);
    } else if (ctx.streamExpression() != null) {
      AssertValue.fail("streamExpression not implemented");
    } else {
      AssertValue.fail("Expecting finite set of operations for assignment expression");
    }

    return List.of();
  }

  private List<IRInstr> processExpression(final String rhsExprResult) {
    final var debugInfo =
        debugInfoCreator.apply(getRecordedSymbolOrException(ctx.expression()).getSourceToken());
    final var exprDetails = new ExprProcessingDetails(ctx.expression(),
        new VariableDetails(rhsExprResult, debugInfo));
    return generators.exprGenerator.apply(exprDetails);
  }

  /**
   * Helper method for processing control flow expressions (switch, while, for, try).
   * <p>
   * Ownership Transfer Pattern:
   * 1. Control flow generator creates `rtn` (RETAINED, not scope-registered)
   * 2. CONTROL_FLOW_CHAIN modifies `rtn`
   * 3. After scope exit, `rtn` is still alive
   * 4. Transfer ownership: STORE rhsExprResult, rtn; RELEASE rtn
   * 5. Caller (VariableMemoryManagement) adds: RETAIN + SCOPE_REGISTER rhsExprResult
   * </p>
   *
   * @param generator       Supplier that generates the control flow expression IR
   * @param returningParam  The returningParam context containing return variable info
   * @param parentCtx       The parent context for debug info
   * @param rhsExprResult   The target variable for ownership transfer
   * @return List of IR instructions including ownership transfer
   */
  private List<IRInstr> processControlFlowExpression(
      final Supplier<List<IRInstr>> generator,
      final EK9Parser.ReturningParamContext returningParam,
      final ParserRuleContext parentCtx,
      final String rhsExprResult) {

    // Generate the control flow expression
    final var instructions = new ArrayList<>(generator.get());

    // Extract return variable name from returningParam
    final var returningParamProcessor = new ReturningParamProcessor(stackContext, generators);
    final var returnVariableName = returningParamProcessor.getReturnVariableName(returningParam);

    // Create debug info for ownership transfer
    final var debugInfo = stackContext.createDebugInfo(parentCtx);

    // Transfer ownership from return variable to assignment target
    // Pattern: STORE target, source; RELEASE source (ownership transfer)
    instructions.add(MemoryInstr.store(rhsExprResult, returnVariableName, debugInfo));
    instructions.add(MemoryInstr.release(returnVariableName, debugInfo));

    // Caller (VariableMemoryManagement) will add: RETAIN + SCOPE_REGISTER rhsExprResult
    return instructions;
  }

  /**
   * Process switch expression: result <- switch value <- rtn <- initialValue ...
   */
  private List<IRInstr> processSwitchExpression(final String rhsExprResult) {
    return processControlFlowExpression(
        () -> generators.switchStatementGenerator.apply(ctx.switchStatementExpression()),
        ctx.switchStatementExpression().returningParam(),
        ctx.switchStatementExpression(),
        rhsExprResult);
  }

  /**
   * Process while/do-while expression: result <- while condition <- rtn <- initialValue ...
   */
  private List<IRInstr> processWhileExpression(final String rhsExprResult) {
    return processControlFlowExpression(
        () -> generators.whileStatementGenerator.apply(ctx.whileStatementExpression()),
        ctx.whileStatementExpression().returningParam(),
        ctx.whileStatementExpression(),
        rhsExprResult);
  }

  /**
   * Process for loop expression: result <- for i in range <- rtn <- initialValue ...
   */
  private List<IRInstr> processForExpression(final String rhsExprResult) {
    return processControlFlowExpression(
        () -> generators.forStatementGenerator.apply(ctx.forStatementExpression()),
        ctx.forStatementExpression().returningParam(),
        ctx.forStatementExpression(),
        rhsExprResult);
  }

  /**
   * Process try-catch expression: result <- try <- rtn <- initialValue ...
   */
  private List<IRInstr> processTryExpression(final String rhsExprResult) {
    return processControlFlowExpression(
        () -> generators.tryCatchStatementGenerator.apply(ctx.tryStatementExpression()),
        ctx.tryStatementExpression().returningParam(),
        ctx.tryStatementExpression(),
        rhsExprResult);
  }
}