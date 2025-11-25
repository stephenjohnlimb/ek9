package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.data.EvaluationVariableDetails;
import org.ek9lang.compiler.ir.data.GuardVariableDetails;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Generates IR for switch statements and expressions using CONTROL_FLOW_CHAIN.
 * Follows the same pattern as IfStatementGenerator.
 * <p>
 * Supports both statement and expression forms:
 * </p>
 * <ul>
 *   <li>Statement form: {@code switch expr} - no return value</li>
 *   <li>Expression form: {@code result <- switch expr} - returns value assigned to return variable</li>
 * </ul>
 * <p>
 * Current implementation supports:
 * - Literal case comparisons (case 1, case 2, etc.)
 * - Integer type evaluation variable
 * - Default case
 * - Guard variables with all operators ({@code <-}, {@code :=}, {@code :=?}, {@code ?=})
 * - Expression form with ARC-safe result assignment
 * </p>
 * <p>
 * For guards with guard checks ({@code <-}, {@code ?=} operators), if the guard fails
 * (null or unset), the switch is skipped entirely (consistent with WHILE/FOR/TRY guards).
 * Note: Guards are NOT supported in expression forms (enforced by semantic analysis).
 * </p>
 * <p>
 * NOT yet supported:
 * - Enum case comparisons
 * - Multiple literals per case (case 1, 2, 3)
 * - Expression cases (case &lt; 12, case &gt; 50)
 * </p>
 */
public final class SwitchStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.SwitchStatementExpressionContext, List<IRInstr>> {

  private final GeneratorSet generators;
  private final LoopGuardHelper loopGuardHelper;
  private final ReturningParamProcessor returningParamProcessor;

  public SwitchStatementGenerator(final IRGenerationContext stackContext,
                                  final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
    this.loopGuardHelper = new LoopGuardHelper(stackContext, generators);
    this.returningParamProcessor = new ReturningParamProcessor(stackContext, generators);
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.SwitchStatementExpressionContext ctx) {
    AssertValue.checkNotNull("SwitchStatementExpressionContext cannot be null", ctx);

    final var debugInfo = stackContext.createDebugInfo(ctx);
    final var isExpressionForm = returningParamProcessor.isExpressionForm(ctx.returningParam());

    // Check for guard
    final var preFlowStmt = ctx.preFlowAndControl().preFlowStatement();
    final boolean hasGuard = preFlowStmt != null;
    final boolean hasControl = ctx.preFlowAndControl().control != null;

    // Route to appropriate generator based on form
    if (isExpressionForm) {
      // Expression form: result <- switch expr
      // Note: Guards are NOT allowed in expression forms (semantic analysis enforces this)
      return generateSwitchExpression(ctx, debugInfo);
    } else if (hasGuard) {
      // Statement form with guard
      return generateSwitchWithGuard(ctx, preFlowStmt, hasControl, debugInfo);
    } else {
      // Statement form without guard
      return generateSwitchWithoutGuard(ctx, debugInfo);
    }
  }

  /**
   * Generate switch without guard - original pattern.
   */
  private List<IRInstr> generateSwitchWithoutGuard(
      final EK9Parser.SwitchStatementExpressionContext ctx,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();

    // Enter chain scope for switch
    final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(chainScopeId, debugInfo));

    // Evaluate switch expression
    final var evalVariable = evaluateSwitchExpressionInline(ctx.preFlowAndControl(), instructions);

    // Process cases and default
    final var conditionChain = new ArrayList<ConditionCaseDetails>();
    for (var caseStmt : ctx.caseStatement()) {
      conditionChain.add(processCaseStatement(caseStmt, evalVariable));
    }

    List<IRInstr> defaultBodyEvaluation = List.of();
    if (ctx.block() != null) {
      defaultBodyEvaluation = processDefaultCase(ctx.block());
    }

    // Create SWITCH chain (no guards)
    final var switchDetails = ControlFlowChainDetails.createSwitchWithGuards(
        null,
        GuardVariableDetails.none(),
        evalVariable.name(),
        evalVariable.type(),
        null, null, List.of(),
        conditionChain,
        defaultBodyEvaluation,
        null,
        debugInfo,
        chainScopeId
    );

    instructions.add(ControlFlowChainInstr.controlFlowChain(switchDetails));

    // Exit chain scope
    instructions.add(ScopeInstr.exit(chainScopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate switch expression form (result <- switch expr).
   * <p>
   * Key differences from statement form:
   * <ul>
   *   <li>Return variable is declared and initialized via returningParam</li>
   *   <li>Each case body assigns its result to the return variable using ARC-safe pattern</li>
   *   <li>Default case also assigns to return variable</li>
   *   <li>Uses SWITCH_EXPRESSION chain type</li>
   * </ul>
   * </p>
   * <p>
   * ARC ownership pattern inside branches (from ternary operator):
   * <pre>
   * RELEASE result        // Release old value
   * STORE result, _temp   // Store branch result
   * RETAIN result         // Ownership transfer (NO SCOPE_REGISTER!)
   * </pre>
   * </p>
   */
  private List<IRInstr> generateSwitchExpression(
      final EK9Parser.SwitchStatementExpressionContext ctx,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();

    // Enter chain scope for entire switch expression
    final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(chainScopeId, debugInfo));

    // Process return variable from returningParam
    // This adds REFERENCE declaration, initial value, RETAIN, SCOPE_REGISTER
    final var returnDetails = returningParamProcessor.process(ctx.returningParam(), chainScopeId);
    instructions.addAll(returnDetails.returnVariableSetup());

    // Evaluate switch expression (what we're switching on)
    final var evalVariable = evaluateSwitchExpressionInline(ctx.preFlowAndControl(), instructions);

    // Process cases with expression result assignment
    final var conditionChain = new ArrayList<ConditionCaseDetails>();
    for (var caseStmt : ctx.caseStatement()) {
      conditionChain.add(processCaseStatementExpression(caseStmt, evalVariable, returnDetails.returnVariable()));
    }

    // Process default case with result assignment
    List<IRInstr> defaultBodyEvaluation = List.of();
    String defaultResult = null;
    if (ctx.block() != null) {
      final var defaultInstructions = processDefaultCaseExpression(ctx.block(), returnDetails.returnVariable());
      defaultBodyEvaluation = defaultInstructions.bodyInstructions();
      defaultResult = defaultInstructions.resultVariable();
    }

    // Create evaluation details for the switch expression
    final var evaluationDetails = EvaluationVariableDetails.withSetup(
        evalVariable.name(),
        evalVariable.type(),
        List.of()  // Setup already done inline
    );

    // Create SWITCH_EXPRESSION chain
    final var switchDetails = ControlFlowChainDetails.createSwitchExpression(
        returnDetails.returnVariable(),  // overall result
        evaluationDetails,
        returnDetails,
        conditionChain,
        defaultBodyEvaluation,
        defaultResult,
        debugInfo,
        chainScopeId
    );

    instructions.add(ControlFlowChainInstr.controlFlowChain(switchDetails));

    // Exit chain scope
    instructions.add(ScopeInstr.exit(chainScopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate switch with guard - uses LoopGuardHelper pattern (consistent with WHILE/FOR/TRY).
   */
  private List<IRInstr> generateSwitchWithGuard(
      final EK9Parser.SwitchStatementExpressionContext ctx,
      final EK9Parser.PreFlowStatementContext preFlowStmt,
      final boolean hasControl,
      final DebugInfo debugInfo) {

    // Create guard scope for entire switch
    final var guardScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(guardScopeId, debugInfo, IRFrameType.BLOCK);

    // Evaluate guard using LoopGuardHelper
    final var guardDetails = loopGuardHelper.evaluateGuardVariable(preFlowStmt, guardScopeId);

    // Build switch body instructions
    final var switchBodyInstructions = new ArrayList<IRInstr>();

    // Enter chain scope for switch evaluation variable
    final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);
    switchBodyInstructions.add(ScopeInstr.enter(chainScopeId, debugInfo));

    // Evaluate switch expression
    final EvalVariable evalVariable;
    if (hasControl) {
      // Form #3: Guard + Control - switch on the control expression
      evalVariable = evaluateSwitchExpressionInline(ctx.preFlowAndControl(), switchBodyInstructions);
    } else {
      // Form #1: Guard only - switch on the guard variable itself
      final var guardSymbol = getRecordedSymbolOrException(preFlowStmt.variableDeclaration() != null
          ? preFlowStmt.variableDeclaration()
          : preFlowStmt.assignmentStatement() != null
          ? preFlowStmt.assignmentStatement().identifier()
          : preFlowStmt.guardExpression().identifier());
      final var guardVarName = new VariableNameForIR().apply(guardSymbol);
      final var guardTypeName = typeNameOrException.apply(guardSymbol);
      // Load guard variable as evaluation variable
      final var evalTemp = stackContext.generateTempName();
      switchBodyInstructions.add(MemoryInstr.load(evalTemp, guardVarName, debugInfo));
      switchBodyInstructions.add(MemoryInstr.retain(evalTemp, debugInfo));
      switchBodyInstructions.add(ScopeInstr.register(evalTemp, chainScopeId, debugInfo));
      evalVariable = new EvalVariable(evalTemp, guardTypeName, guardSymbol);
    }

    // Process cases and default
    final var conditionChain = new ArrayList<ConditionCaseDetails>();
    for (var caseStmt : ctx.caseStatement()) {
      conditionChain.add(processCaseStatement(caseStmt, evalVariable));
    }

    List<IRInstr> defaultBodyEvaluation = List.of();
    if (ctx.block() != null) {
      defaultBodyEvaluation = processDefaultCase(ctx.block());
    }

    // Create SWITCH_WITH_GUARDS chain
    final var switchChainGuardDetails = GuardVariableDetails.create(
        guardDetails.guardVariables(), List.of(), chainScopeId, null);

    final var switchDetails = ControlFlowChainDetails.createSwitchWithGuards(
        null,
        switchChainGuardDetails,
        evalVariable.name(),
        evalVariable.type(),
        null, null, List.of(),
        conditionChain,
        defaultBodyEvaluation,
        null,
        debugInfo,
        chainScopeId
    );

    switchBodyInstructions.add(ControlFlowChainInstr.controlFlowChain(switchDetails));

    // Exit chain scope
    switchBodyInstructions.add(ScopeInstr.exit(chainScopeId, debugInfo));
    stackContext.exitScope();

    // Wrap switch in guard entry check if needed
    final List<IRInstr> instructions;
    if (guardDetails.hasGuardEntryCheck()) {
      instructions = loopGuardHelper.wrapBodyWithGuardEntryCheck(
          guardDetails, switchBodyInstructions, guardScopeId, debugInfo);
    } else {
      // No guard check (e.g., := operator) - just emit with guard setup
      instructions = new ArrayList<>();
      instructions.add(ScopeInstr.enter(guardScopeId, debugInfo));
      instructions.addAll(guardDetails.guardScopeSetup());
      instructions.addAll(switchBodyInstructions);
      instructions.add(ScopeInstr.exit(guardScopeId, debugInfo));
    }

    // Exit guard scope
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
   * Process a case statement for expression form.
   * <p>
   * In EK9 switch expressions, the case blocks contain direct assignments to
   * the return variable (e.g., {@code rtn: "Just A"}). The block statement
   * generator handles the assignment with proper ARC management.
   * </p>
   * <p>
   * No extra result assignment is needed - the case body directly assigns
   * to the return variable which is already registered in the outer scope.
   * </p>
   *
   * @param ctx            The case statement context
   * @param evalVariable   The evaluation variable being compared
   * @param returnVariable The return variable (assigned in body statements)
   * @return ConditionCaseDetails for expression form
   */
  private ConditionCaseDetails processCaseStatementExpression(
      final EK9Parser.CaseStatementContext ctx,
      final EvalVariable evalVariable,
      final String returnVariable) {

    final var debugInfo = stackContext.createDebugInfo(ctx);
    final var caseExpressions = ctx.caseExpression();

    // Create TIGHT condition scope for case comparison(s)
    final var conditionScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(conditionScopeId, debugInfo, IRFrameType.BLOCK);

    final var conditionEvaluation = new ArrayList<IRInstr>();
    conditionEvaluation.add(ScopeInstr.enter(conditionScopeId, debugInfo));

    // Generate OR chain for ALL case expressions (handles single and multiple)
    final var orChainResult = generators.switchCaseOrChainGenerator.generateOrChain(
        caseExpressions, evalVariable, conditionScopeId);

    conditionEvaluation.addAll(orChainResult.instructions());
    conditionEvaluation.add(ScopeInstr.exit(conditionScopeId, debugInfo));
    stackContext.exitScope();

    // Create branch scope for case body
    final var branchScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(branchScopeId, debugInfo, IRFrameType.BLOCK);

    final var bodyEvaluation = new ArrayList<IRInstr>();
    bodyEvaluation.add(ScopeInstr.enter(branchScopeId, debugInfo));

    // Process case body - statements include assignment to return variable
    // The block statement generator handles the assignment with proper ARC
    bodyEvaluation.addAll(processBlockStatements(ctx.block()));

    bodyEvaluation.add(ScopeInstr.exit(branchScopeId, debugInfo));
    stackContext.exitScope();

    // Create condition case with result
    return ConditionCaseDetails.createLiteral(
        branchScopeId,
        conditionEvaluation,
        orChainResult.resultVariable(),
        orChainResult.primitiveCondition(),
        bodyEvaluation,
        returnVariable  // result is the return variable (assigned in body)
    );
  }

  /**
   * Process default case for expression form.
   * <p>
   * In EK9 switch expressions, the default block contains direct assignment
   * to the return variable. No extra result tracking needed.
   * </p>
   *
   * @param blockCtx       The default block context
   * @param returnVariable The return variable (assigned in body statements)
   * @return DefaultCaseResult with instructions and result variable
   */
  private DefaultCaseResult processDefaultCaseExpression(
      final EK9Parser.BlockContext blockCtx,
      final String returnVariable) {

    final var debugInfo = stackContext.createDebugInfo(blockCtx);

    // Create scope for default body
    final var defaultScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(defaultScopeId, debugInfo, IRFrameType.BLOCK);

    final var bodyEvaluation = new ArrayList<IRInstr>();
    bodyEvaluation.add(ScopeInstr.enter(defaultScopeId, debugInfo));

    // Process default body statements - includes assignment to return variable
    bodyEvaluation.addAll(processBlockStatements(blockCtx));

    bodyEvaluation.add(ScopeInstr.exit(defaultScopeId, debugInfo));
    stackContext.exitScope();

    return new DefaultCaseResult(bodyEvaluation, returnVariable);
  }

  /**
   * Record for default case result in expression form.
   */
  private record DefaultCaseResult(List<IRInstr> bodyInstructions, String resultVariable) {
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
   * Record to hold evaluation variable information.
   * Public to allow access from SwitchCaseOrChainGenerator.
   */
  public record EvalVariable(String name, String type, ISymbol typeSymbol) {
  }
}
