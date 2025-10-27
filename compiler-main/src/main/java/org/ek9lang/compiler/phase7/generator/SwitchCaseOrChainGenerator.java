package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.data.LogicalDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.LogicalOperationInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.calls.CallDetailsForIsTrue;
import org.ek9lang.compiler.phase7.support.ConditionalEvaluation;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.LiteralProcessingDetails;
import org.ek9lang.compiler.phase7.support.OperandEvaluation;
import org.ek9lang.compiler.phase7.support.PrimaryReferenceProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.compiler.support.EK9TypeNames;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Generates short-circuit OR chains for multiple switch case expressions.
 * <p>
 * Example: {@code case 'D', 'A', 'Z'} generates:
 * {@code text._eq('D') OR (text._eq('A') OR text._eq('Z'))}
 * </p>
 * <p>
 * Uses LOGICAL_OR_BLOCK for efficient short-circuit evaluation:
 * - If first comparison is TRUE → return immediately (skip remaining)
 * - If FALSE → evaluate next comparison (recursively)
 * </p>
 * <p>
 * Backend Benefits:
 * - JVM: Generates IFNE jumps for early exit
 * - LLVM: Generates conditional branches with proper PHI nodes
 * </p>
 */
public final class SwitchCaseOrChainGenerator extends AbstractGenerator {

  private final GeneratorSet generators;
  private final VariableMemoryManagement variableMemoryManagement;
  private final OperatorMap operatorMap = new OperatorMap();
  private final CallDetailsForIsTrue callDetailsForTrue = new CallDetailsForIsTrue();
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();

  public SwitchCaseOrChainGenerator(final org.ek9lang.compiler.phase7.generation.IRGenerationContext stackContext,
                                    final VariableMemoryManagement variableMemoryManagement,
                                    final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("VariableMemoryManagement cannot be null", variableMemoryManagement);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.variableMemoryManagement = variableMemoryManagement;
    this.generators = generators;
  }

  /**
   * Generate OR chain for multiple case expressions with short-circuit evaluation.
   *
   * @param caseExpressions  List of case expression contexts
   * @param evalVariable     The switch evaluation variable details
   * @param conditionScopeId Scope for condition evaluation
   * @return ComparisonResult with final result variable, instructions, and primitive condition
   */
  public ComparisonResult generateOrChain(
      final List<EK9Parser.CaseExpressionContext> caseExpressions,
      final SwitchStatementGenerator.EvalVariable evalVariable,
      final String conditionScopeId) {

    AssertValue.checkNotNull("Case expressions cannot be null", caseExpressions);
    AssertValue.checkTrue("Must have at least one case expression", !caseExpressions.isEmpty());
    AssertValue.checkNotNull("Eval variable cannot be null", evalVariable);
    AssertValue.checkNotNull("Condition scope ID cannot be null", conditionScopeId);

    if (caseExpressions.size() == 1) {
      // Single case - no OR needed, generate simple comparison
      return generateSingleComparison(caseExpressions.getFirst(), evalVariable, conditionScopeId);
    }

    // Multiple cases - build short-circuit OR chain
    final var allInstructions = new ArrayList<IRInstr>();
    final var orChainResult = buildOrChainRecursive(
        caseExpressions, 0, evalVariable, conditionScopeId, allInstructions);

    // The recursive builder creates the LOGICAL_OR_BLOCK instruction
    // We just need to extract the final primitive boolean
    final var debugInfo = stackContext.createDebugInfo(caseExpressions.getFirst());
    final var finalPrimitive = stackContext.generateTempName();
    final var trueCallDetails = callDetailsForTrue.apply(orChainResult.resultVariable());
    allInstructions.add(CallInstr.call(finalPrimitive, debugInfo, trueCallDetails));

    return new ComparisonResult(
        allInstructions,
        orChainResult.resultVariable(),
        finalPrimitive);
  }

  /**
   * Recursively build OR chain: comparison1 OR (comparison2 OR (comparison3 OR ...))
   */
  private OrChainBuildResult buildOrChainRecursive(
      final List<EK9Parser.CaseExpressionContext> caseExpressions,
      final int currentIndex,
      final SwitchStatementGenerator.EvalVariable evalVariable,
      final String conditionScopeId,
      final List<IRInstr> accumulatedInstructions) {

    final var currentExpr = caseExpressions.get(currentIndex);
    final var debugInfo = stackContext.createDebugInfo(currentExpr);

    // Generate LEFT comparison (current case expression)
    final var leftInstructions = new ArrayList<IRInstr>();
    final var leftComparison = generateSingleComparisonInstructions(
        currentExpr, evalVariable, conditionScopeId, leftInstructions);

    // Extract primitive boolean from left for short-circuit decision
    final var leftPrimitive = stackContext.generateTempName();
    final var leftTrueCall = callDetailsForTrue.apply(leftComparison);
    leftInstructions.add(CallInstr.call(leftPrimitive, debugInfo, leftTrueCall));

    final var leftEvaluation = new OperandEvaluation(leftInstructions, leftComparison);
    final var conditionalEvaluation = new ConditionalEvaluation(List.of(), leftPrimitive);

    // BASE CASE: Last expression - no more OR operations needed
    if (currentIndex == caseExpressions.size() - 1) {
      // Just return the comparison instructions - no OR block needed for last element
      accumulatedInstructions.addAll(leftInstructions);
      return new OrChainBuildResult(leftComparison, leftEvaluation, conditionalEvaluation);
    }

    // RECURSIVE CASE: Build right side (remaining comparisons)
    final var rightInstructions = new ArrayList<IRInstr>();
    final var rightBuildResult = buildOrChainRecursive(
        caseExpressions, currentIndex + 1, evalVariable, conditionScopeId, rightInstructions);

    // Create right evaluation (contains recursive OR chain or single comparison)
    final var rightEvaluation = new OperandEvaluation(
        rightInstructions,
        rightBuildResult.resultVariable());

    // Generate result computation: leftComparison._or(rightResult)
    final var orResult = stackContext.generateTempName();
    final var resultInstructions = new ArrayList<IRInstr>();

    final var booleanType = EK9TypeNames.EK9_BOOLEAN;
    final var orCallDetails = new CallDetails(
        leftComparison, booleanType,
        "_or", List.of(booleanType),
        booleanType, List.of(rightBuildResult.resultVariable()),
        new CallMetaDataDetails(true, 0), false);

    resultInstructions.add(CallInstr.operator(orResult, debugInfo, orCallDetails));

    // Memory management for OR result
    final var resultVariableDetails = new VariableDetails(orResult, debugInfo);
    variableMemoryManagement.apply(() -> resultInstructions, resultVariableDetails);

    final var resultEvaluation = new OperandEvaluation(resultInstructions, orResult);

    // Create LOGICAL_OR_BLOCK instruction wrapping left and right
    final var logicalDetails = new LogicalDetails(
        orResult,              // Result variable
        leftEvaluation,        // Left operand evaluation (first comparison)
        conditionalEvaluation, // Primitive condition for short-circuit
        rightEvaluation,       // Right operand evaluation (recursive chain)
        resultEvaluation,      // OR operation result
        debugInfo,
        conditionScopeId);

    final var logicalOrBlock = LogicalOperationInstr.orOperation(logicalDetails);

    // Add the LOGICAL_OR_BLOCK to accumulated instructions
    accumulatedInstructions.add(logicalOrBlock);

    return new OrChainBuildResult(orResult, leftEvaluation, conditionalEvaluation);
  }

  /**
   * Generate a single case comparison (for use in OR chains or standalone).
   * Returns instructions list and the comparison result variable name.
   */
  private ComparisonResult generateSingleComparison(
      final EK9Parser.CaseExpressionContext caseExpr,
      final SwitchStatementGenerator.EvalVariable evalVariable,
      final String conditionScopeId) {

    final var instructions = new ArrayList<IRInstr>();
    final var comparisonResult = generateSingleComparisonInstructions(
        caseExpr, evalVariable, conditionScopeId, instructions);

    // Extract primitive boolean
    final var debugInfo = stackContext.createDebugInfo(caseExpr);
    final var primitiveCondition = stackContext.generateTempName();
    final var trueCallDetails = callDetailsForTrue.apply(comparisonResult);
    instructions.add(CallInstr.call(primitiveCondition, debugInfo, trueCallDetails));

    return new ComparisonResult(instructions, comparisonResult, primitiveCondition);
  }

  /**
   * Generate a single case comparison with promotion support.
   * Adds instructions to the provided list and returns the comparison result variable name.
   */
  private String generateSingleComparisonInstructions(
      final EK9Parser.CaseExpressionContext caseExpr,
      final SwitchStatementGenerator.EvalVariable evalVariable,
      final String conditionScopeId,
      final List<IRInstr> instructions) {

    final var debugInfo = stackContext.createDebugInfo(caseExpr);

    // LOAD evaluation variable (switch variable)
    final var evalVarLoaded = stackContext.generateTempName();
    instructions.add(MemoryInstr.load(evalVarLoaded, evalVariable.name(), debugInfo));
    instructions.add(MemoryInstr.retain(evalVarLoaded, debugInfo));
    instructions.add(ScopeInstr.register(evalVarLoaded, conditionScopeId, debugInfo));

    // Evaluate case value
    final var caseValue = stackContext.generateTempName();
    final var caseValueDetails = new VariableDetails(caseValue, debugInfo);
    final var caseValueEvaluation = evaluateCaseExpression(caseExpr, caseValueDetails);

    instructions.addAll(
        variableMemoryManagement.apply(
            () -> caseValueEvaluation,
            caseValueDetails));

    // Call comparison operator (e.g., _eq, _lt, _gt) with Phase 3 resolved CallSymbol
    final var comparisonResult = stackContext.generateTempName();

    final var evalVarType = evalVariable.typeSymbol();
    final var caseValueSymbol = getCaseExpressionSymbol(caseExpr);
    final var caseValueType = caseValueSymbol.getType().orElseThrow(
        () -> new CompilerException("Case value has no type"));
    final var returnType = stackContext.getParsedModule().getEk9Types().ek9Boolean();
    final var operator = getComparisonOperator(caseExpr);
    final var methodName = operatorMap.getForward(operator);

    // CallContext with parseContext - CallDetailsBuilder uses Phase 3 resolved CallSymbol
    // This handles Character->String promotion automatically via ParameterPromotionProcessor
    final var callContext = CallContext.forBinaryOperationWithContext(
        evalVarType, caseValueType, returnType, methodName,
        evalVarLoaded, caseValue, conditionScopeId, caseExpr);

    final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);

    // Add promotion instructions (if needed)
    instructions.addAll(callDetailsResult.allInstructions());

    // Generate the operator call
    instructions.add(CallInstr.operator(
        new VariableDetails(comparisonResult, debugInfo),
        callDetailsResult.callDetails()));

    // Memory management for comparison result
    instructions.add(MemoryInstr.retain(comparisonResult, debugInfo));
    instructions.add(ScopeInstr.register(comparisonResult, conditionScopeId, debugInfo));

    return comparisonResult;
  }

  /**
   * Evaluate a case expression and generate IR for it.
   * (Reused from SwitchStatementGenerator pattern)
   */
  private List<IRInstr> evaluateCaseExpression(
      final EK9Parser.CaseExpressionContext caseExpr,
      final VariableDetails variableDetails) {

    if (caseExpr.call() != null) {
      return generators.callGenerator.apply(caseExpr.call(), variableDetails);
    } else if (caseExpr.objectAccessExpression() != null) {
      return generators.objectAccessGenerator.apply(caseExpr.objectAccessExpression(), variableDetails);
    } else if (caseExpr.op != null) {
      return generators.exprGenerator.apply(
          new ExprProcessingDetails(caseExpr.expression(), variableDetails));
    } else if (caseExpr.primary() != null) {
      final var primary = caseExpr.primary();

      if (primary.expression() != null) {
        return generators.exprGenerator.apply(
            new ExprProcessingDetails(primary.expression(), variableDetails));
      } else if (primary.literal() != null) {
        final var literalSymbol = getRecordedSymbolOrException(primary.literal());
        final var literalGenerator = new LiteralGenerator(instructionBuilder);
        return new ArrayList<>(
            literalGenerator.apply(
                new LiteralProcessingDetails(literalSymbol, variableDetails.resultVariable())));
      } else if (primary.identifierReference() != null) {
        final var instructions = new ArrayList<IRInstr>();
        final var identifierSymbol = getRecordedSymbolOrException(primary.identifierReference());
        final var variableName = variableNameForIR.apply(identifierSymbol);
        instructions.add(
            MemoryInstr.load(variableDetails.resultVariable(), variableName, variableDetails.debugInfo()));
        return instructions;
      } else if (primary.primaryReference() != null) {
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
   */
  private String getComparisonOperator(final EK9Parser.CaseExpressionContext caseExpr) {
    if (caseExpr.op == null) {
      return "==";
    }

    final var operatorText = caseExpr.op.getText();
    // Normalize != to <> for OperatorMap lookup
    if ("!=".equals(operatorText)) {
      return "<>";
    }

    return operatorText;
  }

  /**
   * Helper record for building OR chains recursively.
   */
  private record OrChainBuildResult(
      String resultVariable,            // Result variable name
      OperandEvaluation leftEvaluation, // For building parent OR block
      ConditionalEvaluation conditionalEvaluation) {
  }

  /**
   * Result of comparison generation.
   */
  public record ComparisonResult(
      List<IRInstr> instructions,      // All instructions including LOGICAL_OR_BLOCK
      String resultVariable,           // EK9 Boolean result
      String primitiveCondition) {
  }  // primitive boolean for branching
}
