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
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Generates IR for switch statements (statement form only) using CONTROL_FLOW_CHAIN.
 * Follows the same pattern as IfStatementGenerator.
 * <p>
 * Current implementation supports:
 * - Literal case comparisons (case 1, case 2, etc.)
 * - Integer type evaluation variable
 * - Default case
 * - Statement form only (no return value)
 * </p>
 * <p>
 * NOT yet supported:
 * - Expression form (switch as expression returning value)
 * - Guard variables in switch
 * - Enum case comparisons
 * - Multiple literals per case (case 1, 2, 3)
 * - Expression cases (case &lt; 12, case &gt; 50)
 * </p>
 */
public final class SwitchStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.SwitchStatementExpressionContext, List<IRInstr>> {

  private final GeneratorSet generators;

  public SwitchStatementGenerator(final IRGenerationContext stackContext,
                                  final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.SwitchStatementExpressionContext ctx) {
    AssertValue.checkNotNull("SwitchStatementExpressionContext cannot be null", ctx);

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Validate - throw exceptions for unsupported features
    validateSwitchStatementFormOnly(ctx);
    validateNoGuards(ctx.preFlowAndControl());

    // 1. Enter chain scope for entire switch
    final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(chainScopeId, debugInfo));

    // 2. Evaluate switch expression INLINE (in chain scope)
    final var evalVariable = evaluateSwitchExpressionInline(ctx.preFlowAndControl(), instructions);

    // 3. Process each case statement
    final var conditionChain = new ArrayList<ConditionCaseDetails>();
    for (var caseStmt : ctx.caseStatement()) {
      conditionChain.add(processCaseStatement(caseStmt, evalVariable));
    }

    // 4. Process default case (if present)
    List<IRInstr> defaultBodyEvaluation = List.of();
    if (ctx.block() != null) {
      defaultBodyEvaluation = processDefaultCase(ctx.block());
    }

    // 5. Create CONTROL_FLOW_CHAIN details
    final var details = ControlFlowChainDetails.createSwitch(
        null,  // no result (statement form)
        evalVariable.name(),
        evalVariable.type(),
        List.of(),  // no setup - already done inline
        null,  // no return variable
        null,  // no return variable type
        List.of(),  // no return variable setup
        conditionChain,
        defaultBodyEvaluation,
        null,  // no default result
        debugInfo,
        chainScopeId
    );

    // 6. Generate CONTROL_FLOW_CHAIN instruction
    instructions.addAll(generators.controlFlowChainGenerator.apply(details));

    // 7. Exit chain scope
    instructions.add(ScopeInstr.exit(chainScopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Evaluate the switch expression inline in the chain scope.
   * The evaluation variable will live for the entire switch duration.
   *
   * @param ctx          The preFlowAndControl context containing the expression
   * @param instructions List to append evaluation instructions to
   * @return EvalVariable containing the variable name and type
   */
  private EvalVariable evaluateSwitchExpressionInline(
      final EK9Parser.PreFlowAndControlContext ctx,
      final List<IRInstr> instructions) {

    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Generate temporary variable for evaluation result
    final var evalVariable = stackContext.generateTempName();
    final var evalVariableDetails = new VariableDetails(evalVariable, debugInfo);

    // Use variableMemoryManagement to ensure proper RETAIN/SCOPE_REGISTER
    instructions.addAll(
        generators.variableMemoryManagement.apply(
            () -> generators.exprGenerator.apply(
                new ExprProcessingDetails(ctx.expression(), evalVariableDetails)
            ),
            evalVariableDetails
        )
    );

    // Get type from symbol
    final var exprSymbol = getRecordedSymbolOrException(ctx.expression());
    final var evalType = typeNameOrException.apply(exprSymbol);

    return new EvalVariable(evalVariable, evalType, exprSymbol);
  }

  /**
   * Process a case statement and generate condition evaluation + body.
   * Handles single and multiple case comparisons (case 1, 2, 3).
   * Uses short-circuit OR logic for multiple cases via LOGICAL_OR_BLOCK.
   * <p>
   * CRITICAL: All EK9 object operations get RETAIN + SCOPE_REGISTER:
   * - LOAD evaluation variable
   * - LOAD_LITERAL case value
   * - CALL _eq operator result
   * </p>
   */
  private ConditionCaseDetails processCaseStatement(
      final EK9Parser.CaseStatementContext ctx,
      final EvalVariable evalVariable) {

    final var debugInfo = stackContext.createDebugInfo(ctx);
    final var caseExpressions = ctx.caseExpression();

    // Create TIGHT condition scope for case comparison(s)
    final var conditionScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(conditionScopeId, debugInfo, IRFrameType.BLOCK);

    final var conditionEvaluation = new ArrayList<IRInstr>();
    conditionEvaluation.add(ScopeInstr.enter(conditionScopeId, debugInfo));

    // Generate OR chain for ALL case expressions (handles single and multiple)
    // Uses short-circuit evaluation via LOGICAL_OR_BLOCK for efficiency
    final var orChainResult = generators.switchCaseOrChainGenerator.generateOrChain(
        caseExpressions, evalVariable, conditionScopeId);

    // Add all comparison instructions (includes LOGICAL_OR_BLOCK for multiple cases)
    conditionEvaluation.addAll(orChainResult.instructions());

    // Exit condition scope immediately - frees all condition temps
    conditionEvaluation.add(ScopeInstr.exit(conditionScopeId, debugInfo));
    stackContext.exitScope();

    // Create branch scope for case body
    final var branchScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(branchScopeId, debugInfo, IRFrameType.BLOCK);

    // Wrap body evaluation with SCOPE_ENTER/EXIT
    final var bodyEvaluation = new ArrayList<IRInstr>();
    bodyEvaluation.add(ScopeInstr.enter(branchScopeId, debugInfo));
    bodyEvaluation.addAll(processBlockStatements(ctx.block()));
    bodyEvaluation.add(ScopeInstr.exit(branchScopeId, debugInfo));

    // Exit branch scope
    stackContext.exitScope();

    // Create condition case (statement form has no body result)
    return ConditionCaseDetails.createLiteral(
        branchScopeId,
        conditionEvaluation,
        orChainResult.resultVariable(),    // EK9 Boolean result (from OR chain)
        orChainResult.primitiveCondition(),  // primitive boolean for backend
        bodyEvaluation,
        null  // no result for statement form
    );
  }

  /**
   * Process default case block with its own branch scope.
   */
  private List<IRInstr> processDefaultCase(final EK9Parser.BlockContext blockCtx) {
    final var debugInfo = stackContext.createDebugInfo(blockCtx);

    // Create scope for default body
    final var defaultScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(defaultScopeId, debugInfo, IRFrameType.BLOCK);

    // Wrap body evaluation with SCOPE_ENTER/EXIT
    final var bodyEvaluation = new ArrayList<IRInstr>();
    bodyEvaluation.add(ScopeInstr.enter(defaultScopeId, debugInfo));
    bodyEvaluation.addAll(processBlockStatements(blockCtx));
    bodyEvaluation.add(ScopeInstr.exit(defaultScopeId, debugInfo));

    // Exit default scope
    stackContext.exitScope();

    return bodyEvaluation;
  }

  /**
   * Process all block statements in a block context.
   * Same pattern as IfStatementGenerator.
   */
  private List<IRInstr> processBlockStatements(final EK9Parser.BlockContext blockCtx) {
    final var instructions = new ArrayList<IRInstr>();
    for (var blockStatement : blockCtx.instructionBlock().blockStatement()) {
      instructions.addAll(generators.blockStmtGenerator.apply(blockStatement));
    }
    return instructions;
  }

  /**
   * Validate that this is statement form only (no return value).
   */
  private void validateSwitchStatementFormOnly(final EK9Parser.SwitchStatementExpressionContext ctx) {
    validateStatementFormOnly(ctx.returningParam(), "Switch");
  }

  /**
   * Validate that there are no guard variables.
   */
  private void validateNoGuards(final EK9Parser.PreFlowAndControlContext ctx) {
    validateNoPreFlowStatement(ctx.preFlowStatement(), "Switch");
  }

  /**
   * Record to hold evaluation variable information.
   * Public to allow access from SwitchCaseOrChainGenerator.
   */
  public record EvalVariable(String name, String type, ISymbol typeSymbol) {
  }
}
