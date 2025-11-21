package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.data.GuardVariableDetails;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
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

    // 1. Enter chain scope for entire switch
    // SWITCH needs manual scope management because evaluation variable lives for entire switch
    final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(chainScopeId, debugInfo));

    // 2. Check for three forms (from grammar preFlowAndControl):
    //    Form #1: preFlowStatement only (guard becomes switch expression)
    //    Form #2: control=expression only (explicit switch expression)
    //    Form #3: preFlowStatement (WITH|THEN) control=expression (guard + explicit control)
    final EvalVariable evalVariable;
    String guardVariableName = null;  // Track guard variable name for chain type
    final boolean hasGuard = ctx.preFlowAndControl().preFlowStatement() != null;
    final boolean hasControl = ctx.preFlowAndControl().control != null;

    if (hasGuard && hasControl) {
      // Form #3: Guard + Control
      // Example: switch opt <- getOpt() then opt.get()
      // Guard declares variable, control specifies what to switch on
      final var guardResult = evaluateGuardVariableInline(ctx.preFlowAndControl(), instructions);
      guardVariableName = guardResult.name();
      evalVariable = evaluateSwitchExpressionInline(ctx.preFlowAndControl(), instructions);
    } else if (hasGuard) {
      // Form #1: Guard only
      // Example: switch value <- getValue()
      // Guard variable becomes the switch expression implicitly
      evalVariable = evaluateGuardVariableInline(ctx.preFlowAndControl(), instructions);
      guardVariableName = evalVariable.name();
    } else {
      // Form #2: Control only
      // Example: switch myValue
      // Explicit switch expression
      evalVariable = evaluateSwitchExpressionInline(ctx.preFlowAndControl(), instructions);
    }

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
    // Uses createSwitchWithGuards to produce SWITCH_WITH_GUARDS when guards present
    // Pass guard variable name so hasGuardVariables() returns true for guards
    final var guardDetails = guardVariableName != null
        ? GuardVariableDetails.create(List.of(guardVariableName), List.of(), chainScopeId, null)
        : GuardVariableDetails.none();

    final var details = ControlFlowChainDetails.createSwitchWithGuards(
        null,  // no result (statement form)
        guardDetails,
        evalVariable.name(),
        evalVariable.type(),
        null,  // no return variable
        null,  // no return variable type
        List.of(),  // no return variable setup
        conditionChain,
        defaultBodyEvaluation,
        null,  // no default result
        debugInfo,
        chainScopeId
    );

    // 6. Generate CONTROL_FLOW_CHAIN instruction (just the chain, not scope)
    instructions.add(org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr.controlFlowChain(details));

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
   * Evaluate guard variable for switch statement inline.
   * Pattern: switch value <- getValue()
   * <p>
   * Uses existing variableDeclGenerator to handle the variable declaration,
   * then extracts the guard variable information to use as switch expression.
   * </p>
   *
   * @param ctx          preFlowAndControl context with guard
   * @param instructions List to append guard evaluation instructions
   * @return EvalVariable with guard variable name and type
   */
  private EvalVariable evaluateGuardVariableInline(
      final EK9Parser.PreFlowAndControlContext ctx,
      final List<IRInstr> instructions) {

    final var guardStmt = ctx.preFlowStatement();

    // Use existing variable declaration generator (handles REFERENCE, STORE, RETAIN, SCOPE_REGISTER)
    if (guardStmt.variableDeclaration() != null) {
      instructions.addAll(generators.variableDeclGenerator.apply(guardStmt.variableDeclaration()));

      // Extract guard variable information
      final var guardSymbol = getRecordedSymbolOrException(guardStmt.variableDeclaration());
      final var guardVarName = new VariableNameForIR().apply(guardSymbol);
      final var guardTypeName = typeNameOrException.apply(guardSymbol);

      return new EvalVariable(guardVarName, guardTypeName, guardSymbol);
    }

    // For now, only support variable declaration form (value <- expr)
    // Future: Support assignment (:=) and guarded assignment (?=) forms
    throw new org.ek9lang.core.CompilerException(
        "Switch guards currently only support variable declaration form (value <- expr)");
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
   * Record to hold evaluation variable information.
   * Public to allow access from SwitchCaseOrChainGenerator.
   */
  public record EvalVariable(String name, String type, ISymbol typeSymbol) {
  }
}
