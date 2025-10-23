package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.BooleanExtractionParams;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.LiteralProcessingDetails;
import org.ek9lang.compiler.phase7.support.PrimaryReferenceProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

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
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final OperatorMap operatorMap = new OperatorMap();
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();

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
    validateStatementFormOnly(ctx);
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
   * @param ctx The preFlowAndControl context containing the expression
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
   * Handles literal case comparisons only (case 1, case 2, etc.).
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

    // Validate - only support single literal case for now
    if (ctx.caseExpression().size() > 1) {
      throw new CompilerException("Multiple literals per case not yet implemented");
    }

    final var caseExpr = ctx.caseExpression(0);

    // Validate - no expression operators for now
    if (caseExpr.op != null) {
      throw new CompilerException("Expression cases (case < 12) not yet implemented");
    }

    // Create TIGHT condition scope for case comparison
    final var conditionScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(conditionScopeId, debugInfo, IRFrameType.BLOCK);

    final var conditionEvaluation = new ArrayList<IRInstr>();
    conditionEvaluation.add(ScopeInstr.enter(conditionScopeId, debugInfo));

    // LOAD evaluation variable (with memory management)
    final var evalVarLoaded = stackContext.generateTempName();
    conditionEvaluation.add(MemoryInstr.load(evalVarLoaded, evalVariable.name(), debugInfo));
    conditionEvaluation.add(MemoryInstr.retain(evalVarLoaded, debugInfo));
    conditionEvaluation.add(ScopeInstr.register(evalVarLoaded, conditionScopeId, debugInfo));

    // Evaluate case value based on the type of case expression
    // (can be call, objectAccessExpression, operator+expression, or primary)
    final var caseValue = stackContext.generateTempName();
    final var caseValueDetails = new VariableDetails(caseValue, debugInfo);
    final var caseValueEvaluation = evaluateCaseExpression(caseExpr, caseValueDetails);

    conditionEvaluation.addAll(
        generators.variableMemoryManagement.apply(
            () -> caseValueEvaluation,
            caseValueDetails
        )
    );

    // Call _eq operator: evalVarLoaded._eq(caseValue)
    // CRITICAL: CALL result needs memory management!
    final var comparisonResult = stackContext.generateTempName();

    // Get symbols for call context
    final var evalVarTypeSymbol = evalVariable.typeSymbol();
    final var caseValueSymbol = getCaseExpressionSymbol(caseExpr);

    // Get return type - Boolean (use cached type from Ek9Types)
    final var returnType = stackContext.getParsedModule().getEk9Types().ek9Boolean();

    // Get operator and method name from operator map
    final var operator = getComparisonOperator(caseExpr);
    final var methodName = operatorMap.getForward(operator);

    // Create call context for comparison operator
    final var callContext = CallContext.forBinaryOperation(
        evalVarTypeSymbol,          // Target type (evaluation variable type)
        caseValueSymbol,            // Argument type (case value)
        returnType,                 // Return type (Boolean)
        methodName,                 // Method name (_eq, _lt, _gt, _neq, etc.)
        evalVarLoaded,              // Target variable
        caseValue,                  // Argument variable
        conditionScopeId            // Current scope ID
    );

    // Use CallDetailsBuilder for cost-based method resolution
    final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);

    // Add any promotion instructions
    conditionEvaluation.addAll(callDetailsResult.allInstructions());

    // Generate the operator call
    conditionEvaluation.add(CallInstr.operator(
        new VariableDetails(comparisonResult, debugInfo),
        callDetailsResult.callDetails()
    ));

    // CRITICAL: CALL result gets RETAIN + SCOPE_REGISTER
    conditionEvaluation.add(MemoryInstr.retain(comparisonResult, debugInfo));
    conditionEvaluation.add(ScopeInstr.register(comparisonResult, conditionScopeId, debugInfo));

    // Extract primitive boolean for backend
    final var primitiveCondition = stackContext.generateTempName();
    final var extractionParams = new BooleanExtractionParams(
        comparisonResult, primitiveCondition, debugInfo);
    conditionEvaluation.addAll(generators.primitiveBooleanExtractor.apply(extractionParams));

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
        comparisonResult,    // EK9 Boolean result
        primitiveCondition,  // primitive boolean for backend
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
  private void validateStatementFormOnly(final EK9Parser.SwitchStatementExpressionContext ctx) {
    if (ctx.returningParam() != null) {
      throw new CompilerException("Switch expression form not yet implemented");
    }
  }

  /**
   * Validate that there are no guard variables.
   */
  private void validateNoGuards(final EK9Parser.PreFlowAndControlContext ctx) {
    if (ctx.preFlowStatement() != null) {
      throw new CompilerException("Switch with guard variables not yet implemented");
    }
  }

  /**
   * Evaluate a case expression and generate IR for it.
   * caseExpression can be: call | objectAccessExpression | op expression | primary
   *
   * @param caseExpr The case expression context
   * @param variableDetails Variable to store the result
   * @return List of IR instructions to evaluate the case expression
   */
  private List<IRInstr> evaluateCaseExpression(
      final EK9Parser.CaseExpressionContext caseExpr,
      final VariableDetails variableDetails) {

    if (caseExpr.call() != null) {
      // Function/method call: someFunction()
      return generators.callGenerator.apply(caseExpr.call(), variableDetails);
    } else if (caseExpr.objectAccessExpression() != null) {
      // Object access: obj.property or obj.method()
      return generators.objectAccessGenerator.apply(caseExpr.objectAccessExpression(), variableDetails);
    } else if (caseExpr.op != null) {
      // Operator case: < 12, > 50, == "hello"
      // This needs special handling - the operator modifies the comparison, not the value
      // For now, just evaluate the expression
      return generators.exprGenerator.apply(
          new ExprProcessingDetails(caseExpr.expression(), variableDetails));
    } else if (caseExpr.primary() != null) {
      // Literal/identifier case: 1, 2, "hello", someVar
      // Primary includes: (expression), literal, identifierReference, THIS, SUPER
      final var primary = caseExpr.primary();

      if (primary.expression() != null) {
        // Parenthesized expression
        return generators.exprGenerator.apply(
            new ExprProcessingDetails(primary.expression(), variableDetails));
      } else if (primary.literal() != null) {
        // Literal value: 1, "hello", true, etc.
        final var literalSymbol = getRecordedSymbolOrException(primary.literal());
        final var literalGenerator = new LiteralGenerator(instructionBuilder);
        return new ArrayList<>(
            literalGenerator.apply(
                new LiteralProcessingDetails(literalSymbol, variableDetails.resultVariable())));
      } else if (primary.identifierReference() != null) {
        // Variable reference
        final var instructions = new ArrayList<IRInstr>();
        final var identifierSymbol = getRecordedSymbolOrException(primary.identifierReference());
        final var variableName = variableNameForIR.apply(identifierSymbol);
        instructions.add(
            MemoryInstr.load(variableDetails.resultVariable(), variableName, variableDetails.debugInfo()));
        return instructions;
      } else if (primary.primaryReference() != null) {
        // THIS or SUPER
        final var processingDetails =
            new PrimaryReferenceProcessingDetails(primary.primaryReference(), variableDetails.resultVariable());
        return generators.primaryReferenceGenerator.apply(processingDetails);
      } else {
        throw new CompilerException("Unknown primary type in case expression");
      }
    } else {
      throw new CompilerException("Unknown case expression type");
    }
  }

  /**
   * Get the symbol for the case expression for use in CallContext.
   *
   * @param caseExpr The case expression context
   * @return The symbol for the case expression value
   */
  private ISymbol getCaseExpressionSymbol(final EK9Parser.CaseExpressionContext caseExpr) {
    if (caseExpr.call() != null) {
      return getRecordedSymbolOrException(caseExpr.call());
    } else if (caseExpr.objectAccessExpression() != null) {
      return getRecordedSymbolOrException(caseExpr.objectAccessExpression());
    } else if (caseExpr.op != null) {
      return getRecordedSymbolOrException(caseExpr.expression());
    } else if (caseExpr.primary() != null) {
      return getRecordedSymbolOrException(caseExpr.primary());
    } else {
      throw new CompilerException("Unknown case expression type");
    }
  }

  /**
   * Get the operator to use for case comparison.
   * For literal cases (case 1), defaults to equality ("==").
   * For expression cases (case < 10), extracts and normalizes the operator.
   * Normalizes "!=" to "<>" for OperatorMap compatibility.
   *
   * @param caseExpr The case expression context
   * @return The operator string for use with OperatorMap.getForward()
   */
  private String getComparisonOperator(final EK9Parser.CaseExpressionContext caseExpr) {
    if (caseExpr.op == null) {
      // Literal case - no operator specified, default to equality
      return "==";
    }

    // Expression case - extract operator text
    final var operatorText = caseExpr.op.getText();

    // Normalize != to <> for OperatorMap lookup
    // Both are "not equals" but OperatorMap only has <>
    if ("!=".equals(operatorText)) {
      return "<>";
    }

    return operatorText;
  }

  /**
   * Record to hold evaluation variable information.
   */
  private record EvalVariable(String name, String type, ISymbol typeSymbol) {
  }
}
