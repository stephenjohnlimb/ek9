package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.BooleanExtractionParams;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Evaluates conditions with optional guard variables for all control flow constructs.
 * <p>
 * Handles 4 distinct cases:
 * </p>
 * <ol>
 *   <li>Guard WITH condition AND needs check (e.g., {@code <-}, {@code ?=}) -
 *       Generates LOGICAL_AND_BLOCK with short-circuit evaluation</li>
 *   <li>Guard WITH condition but NO check needed (e.g., {@code :=}, {@code =}) -
 *       Just evaluates the condition</li>
 *   <li>Condition only (no guard) - Just evaluates the condition</li>
 *   <li>Guard only (implicit isSet check) AND needs check -
 *       Generates QUESTION_OPERATOR for NULL + isSet checks</li>
 * </ol>
 * <p>
 * This provides consistent guard semantics across all control flow constructs
 * (IF, WHILE, DO-WHILE, SWITCH, TRY) with 90-95% null safety enforcement.
 * </p>
 * <p>
 * The guard double-check pattern ensures:
 * </p>
 * <ol>
 *   <li>IS_NULL check (if null → return false)</li>
 *   <li>_isSet() check (if not null → check if set)</li>
 *   <li>Variable usage (only if both checks pass)</li>
 * </ol>
 */
public final class GuardedConditionEvaluator extends AbstractGenerator {
  private final GeneratorSet generators;

  public GuardedConditionEvaluator(final IRGenerationContext stackContext,
                                   final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
  }

  /**
   * Evaluates a condition expression that may have guard variables.
   * <p>
   * This is the main entry point that handles all 4 cases of guard/condition combinations.
   * The returned ConditionEvaluation contains both the IR instructions for evaluating
   * the condition and the name of the primitive boolean variable for backend branching.
   * </p>
   *
   * @param conditionExpr   Optional explicit condition expression (may be null for guard-only)
   * @param preFlowStmt     Optional guard variable declaration/assignment (may be null for condition-only)
   * @param conditionResult Variable to store the condition result (EK9 Boolean type)
   * @param scopeId         Scope ID where condition evaluation happens
   * @param debugInfo       Debug information for error reporting
   * @return ConditionEvaluation containing instructions and primitive condition variable name
   * @throws CompilerException if neither condition nor guard is provided
   */
  public ConditionEvaluation evaluate(
      final EK9Parser.ExpressionContext conditionExpr,
      final EK9Parser.PreFlowStatementContext preFlowStmt,
      final VariableDetails conditionResult,
      final String scopeId,
      final DebugInfo debugInfo) {

    AssertValue.checkNotNull("conditionResult cannot be null", conditionResult);
    AssertValue.checkNotNull("scopeId cannot be null", scopeId);
    // Note: debugInfo can be null in some test scenarios where ParseTree lacks position info

    // Determine guard operator and check requirements
    final var operatorType = getGuardOperatorType(preFlowStmt);
    final var needsGuardCheck = requiresGuardCheck(operatorType);
    final var hasGuardSetup = preFlowStmt != null;

    final var conditionEvaluation = new ArrayList<IRInstr>();

    // Case 1: Guard WITH explicit condition AND operator requires guard check (e.g., <-, ?=)
    // Use LOGICAL_AND_BLOCK for short-circuit evaluation (condition only evaluated if guard succeeds)
    if (hasGuardSetup && conditionExpr != null && needsGuardCheck) {
      evaluateGuardWithCondition(conditionExpr, preFlowStmt, conditionResult,
          scopeId, debugInfo, conditionEvaluation);
    } else if (hasGuardSetup && conditionExpr != null) {
      evaluateConditionOnly(conditionExpr, conditionResult, conditionEvaluation);
    } else if (conditionExpr != null) {
      evaluateConditionOnly(conditionExpr, conditionResult, conditionEvaluation);
    } else if (hasGuardSetup && needsGuardCheck) {
      evaluateGuardOnly(preFlowStmt, conditionResult, debugInfo, conditionEvaluation);
    } else {
      throw new CompilerException("Control flow must have either a condition or a guard variable");
    }

    // Extract primitive boolean for backend branching
    final var primitiveCondition = extractPrimitiveBoolean(
        conditionResult.resultVariable(), debugInfo, conditionEvaluation);

    return new ConditionEvaluation(conditionEvaluation, primitiveCondition);
  }

  /**
   * Case 1: Evaluates guard WITH explicit condition using LOGICAL_AND_BLOCK.
   * <p>
   * This generates short-circuit evaluation:
   * </p>
   * <ol>
   *   <li>LEFT: Evaluate guard (IS_NULL + _isSet() checks)</li>
   *   <li>CONDITIONAL: Convert guard result to primitive boolean</li>
   *   <li>RIGHT: Evaluate condition (ONLY if guard succeeds)</li>
   *   <li>RESULT: AND guard with condition</li>
   * </ol>
   * <p>
   * This ensures the guard variable is verified as NOT NULL and isSet
   * BEFORE the condition expression can use it.
   * </p>
   */
  private void evaluateGuardWithCondition(
      final EK9Parser.ExpressionContext conditionExpr,
      final EK9Parser.PreFlowStatementContext preFlowStmt,
      final VariableDetails conditionResult,
      final String scopeId,
      final DebugInfo debugInfo,
      final List<IRInstr> conditionEvaluation) {

    // Get the guard variable info
    final var guardVarSymbol = getGuardSymbol(preFlowStmt);

    final var booleanType = stackContext.getParsedModule().getEk9Types().ek9Boolean();
    final var booleanTypeName = typeNameOrException.apply(booleanType);

    // LEFT EVALUATION: Generate question operator with explicit IS_NULL check
    final var guardCheckTemp = stackContext.generateTempName();
    final var leftEvaluationInstructions = new ArrayList<>(
        generators.controlFlowChainGenerator.generateQuestionOperatorForVariable(
            guardVarSymbol, guardCheckTemp, debugInfo));

    // CONDITIONAL EVALUATION: Convert guard result to primitive boolean for short-circuit check
    final var guardPrimitive = stackContext.generateTempName();
    final var guardTrueCallDetails = new org.ek9lang.compiler.ir.data.CallDetails(
        guardCheckTemp, booleanTypeName, "_true", List.of(), "boolean", List.of(),
        new org.ek9lang.compiler.ir.data.CallMetaDataDetails(true, 0), false);
    leftEvaluationInstructions.add(org.ek9lang.compiler.ir.instructions.CallInstr.call(
        guardPrimitive, debugInfo, guardTrueCallDetails));

    // RIGHT EVALUATION: Generate explicit condition (ONLY evaluated if guard succeeds)
    final var explicitCondTemp = stackContext.generateTempName();
    final var explicitCondDetails = new VariableDetails(explicitCondTemp, debugInfo);
    final var rightEvaluationInstructions = new ArrayList<>(
        generators.variableMemoryManagement.apply(
            () -> generators.exprGenerator.apply(
                new ExprProcessingDetails(conditionExpr, explicitCondDetails)
            ),
            explicitCondDetails
        )
    );

    // RESULT EVALUATION: AND guard with condition
    final var resultTemp = stackContext.generateTempName();
    final var andCallDetails = new org.ek9lang.compiler.ir.data.CallDetails(
        guardCheckTemp, booleanTypeName, "_and", List.of(booleanTypeName), booleanTypeName,
        List.of(explicitCondTemp), new org.ek9lang.compiler.ir.data.CallMetaDataDetails(true, 0), false);
    final var resultComputationInstructions = new ArrayList<IRInstr>();
    resultComputationInstructions.add(org.ek9lang.compiler.ir.instructions.CallInstr.operator(
        resultTemp, debugInfo, andCallDetails));

    final var resultVariableDetails = new VariableDetails(resultTemp, debugInfo);
    generators.variableMemoryManagement.apply(() -> resultComputationInstructions, resultVariableDetails);

    // Create LOGICAL_AND_BLOCK with short-circuit evaluation
    final var leftEvaluation = new org.ek9lang.compiler.phase7.support.OperandEvaluation(
        leftEvaluationInstructions, guardCheckTemp);
    final var conditionalEvaluation = new org.ek9lang.compiler.phase7.support.ConditionalEvaluation(
        List.of(), guardPrimitive);
    final var rightEvaluation = new org.ek9lang.compiler.phase7.support.OperandEvaluation(
        rightEvaluationInstructions, explicitCondTemp);
    final var resultEvaluation = new org.ek9lang.compiler.phase7.support.OperandEvaluation(
        resultComputationInstructions, resultTemp);

    final var logicalDetails = new org.ek9lang.compiler.ir.data.LogicalDetails(
        conditionResult.resultVariable(),
        leftEvaluation,
        conditionalEvaluation,
        rightEvaluation,
        resultEvaluation,
        debugInfo,
        scopeId);

    final var logicalAndBlock =
        org.ek9lang.compiler.ir.instructions.LogicalOperationInstr.andOperation(logicalDetails);
    conditionEvaluation.add(logicalAndBlock);
  }

  /**
   * Cases 2 & 3: Evaluates condition expression only (no guard check needed or no guard at all).
   * <p>
   * This is the simple case - just evaluate the condition expression directly
   * with proper memory management (RETAIN/RELEASE/SCOPE_REGISTER).
   * </p>
   */
  private void evaluateConditionOnly(
      final EK9Parser.ExpressionContext conditionExpr,
      final VariableDetails conditionResult,
      final List<IRInstr> conditionEvaluation) {

    conditionEvaluation.addAll(
        generators.variableMemoryManagement.apply(
            () -> generators.exprGenerator.apply(
                new ExprProcessingDetails(conditionExpr, conditionResult)
            ),
            conditionResult
        )
    );
  }

  /**
   * Case 4: Evaluates guard only (implicit isSet check).
   * <p>
   * Generates QUESTION_OPERATOR that performs the double-check pattern:
   * </p>
   * <ol>
   *   <li>IS_NULL check - if null, returns false</li>
   *   <li>_isSet() check - if not null, checks if value is set</li>
   * </ol>
   * <p>
   * This becomes the entire condition - the control flow continues
   * only if the guard variable passes both checks.
   * </p>
   */
  private void evaluateGuardOnly(
      final EK9Parser.PreFlowStatementContext preFlowStmt,
      final VariableDetails conditionResult,
      final DebugInfo debugInfo,
      final List<IRInstr> conditionEvaluation) {

    final var guardVarSymbol = getGuardSymbol(preFlowStmt);

    // Generate question operator with explicit IS_NULL check
    // This replaces UnaryOperatorParams to emit IS_NULL instruction in IR
    conditionEvaluation.addAll(
        generators.controlFlowChainGenerator.generateQuestionOperatorForVariable(
            guardVarSymbol, conditionResult.resultVariable(), debugInfo));
  }

  /**
   * Extracts a primitive boolean from an EK9 Boolean type for backend branching.
   * <p>
   * The backend (bytecode generator) needs a primitive boolean to generate
   * branch instructions (IFEQ, IFNE, etc.). This calls the Boolean._true()
   * method to convert the EK9 Boolean object to a primitive boolean.
   * </p>
   */
  private String extractPrimitiveBoolean(
      final String booleanVariable,
      final DebugInfo debugInfo,
      final List<IRInstr> conditionEvaluation) {

    final var primitiveCondition = stackContext.generateTempName();
    final var extractionParams = new BooleanExtractionParams(
        booleanVariable, primitiveCondition, debugInfo);
    conditionEvaluation.addAll(generators.primitiveBooleanExtractor.apply(extractionParams));
    return primitiveCondition;
  }

  /**
   * Gets the symbol from a preFlowStatement (handles variableDeclaration, assignmentStatement, guardExpression).
   *
   * @param preFlowStmt The preFlowStatement context
   * @return The recorded symbol for the guard variable
   */
  private ISymbol getGuardSymbol(final EK9Parser.PreFlowStatementContext preFlowStmt) {
    if (preFlowStmt == null) {
      throw new CompilerException("preFlowStatement cannot be null");
    }

    if (preFlowStmt.variableDeclaration() != null) {
      return getRecordedSymbolOrException(preFlowStmt.variableDeclaration());
    }

    if (preFlowStmt.assignmentStatement() != null) {
      // Symbol is recorded on the identifier, not the statement
      return getRecordedSymbolOrException(preFlowStmt.assignmentStatement().identifier());
    }

    if (preFlowStmt.guardExpression() != null) {
      // Symbol is recorded on the identifier, not the expression
      return getRecordedSymbolOrException(preFlowStmt.guardExpression().identifier());
    }

    throw new CompilerException(
        "Invalid preFlowStatement - expected variableDeclaration, assignmentStatement, or guardExpression");
  }

  /**
   * Determines the guard operator type from a preFlowStatement.
   * Returns the operator token type, or null if not a guard pattern.
   * Package-private to allow IfStatementGenerator to use for guard variable collection.
   *
   * @param preFlowStmt The preFlowStatement context
   * @return The operator token type (LEFT_ARROW, ASSIGN, ASSIGN2, GUARD, ASSIGN_UNSET), or null
   */
  Integer getGuardOperatorType(final EK9Parser.PreFlowStatementContext preFlowStmt) {
    if (preFlowStmt == null) {
      return null;
    }

    // Check variableDeclaration: <-, :=, =, etc.
    if (preFlowStmt.variableDeclaration() != null) {
      final var varDecl = preFlowStmt.variableDeclaration();
      if (varDecl.op != null) {
        return varDecl.op.getType();
      }
    }

    // Check assignmentStatement: :=, :=?, etc.
    if (preFlowStmt.assignmentStatement() != null) {
      final var assignStmt = preFlowStmt.assignmentStatement();
      if (assignStmt.op != null) {
        return assignStmt.op.getType();
      }
    }

    // Check guardExpression: ?=
    if (preFlowStmt.guardExpression() != null) {
      final var guardExpr = preFlowStmt.guardExpression();
      if (guardExpr.op != null) {
        return guardExpr.op.getType();
      }
    }

    return null;
  }

  /**
   * Checks if the given operator requires guard checks (null check + isSet check).
   * Returns true for operators that need guard checks: {@code <-}, {@code ?=}
   * Returns false for operators that don't: {@code :=}, {@code =}
   * Package-private to allow IfStatementGenerator to use for guard variable collection.
   *
   * @param operatorType The operator token type
   * @return true if guard checks are required, false otherwise
   */
  boolean requiresGuardCheck(final Integer operatorType) {
    if (operatorType == null) {
      return false;
    }

    // LEFT_ARROW (<-) - Declaration guard - WITH guard check
    if (operatorType == EK9Parser.LEFT_ARROW) {
      return true;
    }

    // GUARD (?=) - Guarded assignment - WITH guard check
    if (operatorType == EK9Parser.GUARD) {
      return true;
    }

    // ASSIGN (:=) or ASSIGN2 (=) - Blind assignment - NO guard check
    if (operatorType == EK9Parser.ASSIGN || operatorType == EK9Parser.ASSIGN2) {
      return false;
    }

    // ASSIGN_UNSET (:=?) - Assignment if unset - WITH guard check on result
    // After conditional assignment (only if left was unset), check if result is now set
    return operatorType == EK9Parser.ASSIGN_UNSET;
  }
}
