package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.data.DefaultCaseDetails;
import org.ek9lang.compiler.ir.data.EvaluationVariableDetails;
import org.ek9lang.compiler.ir.data.GuardVariableDetails;
import org.ek9lang.compiler.ir.data.ReturnVariableDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.calls.BooleanNotCallDetailsCreator;
import org.ek9lang.compiler.phase7.calls.CallDetailsForIsTrue;
import org.ek9lang.compiler.phase7.calls.CallDetailsForOfFalse;
import org.ek9lang.compiler.phase7.calls.IsSetCallDetailsCreator;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.BooleanFalseEvaluationCreator;
import org.ek9lang.compiler.phase7.support.BooleanNotEvaluationCreator;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IsSetEvaluationCreator;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Unified generator for all EK9 control flow constructs using CONTROL_FLOW_CHAIN.
 * <p>
 * This generator replaces multiple specialized generators:<br>
 * - QuestionBlockGenerator → generateQuestionOperator()<br>
 * - GuardedAssignmentBlockGenerator → generateGuardedAssignment()<br>
 * - Future: If/else and switch statement generators<br>
 * </p>
 * <p>
 * Key architectural benefits:<br>
 * - Single source of truth for all control flow logic<br>
 * - Consistent memory management patterns across all constructs<br>
 * - Unified optimization metadata for backend code generation<br>
 * - Reduced code duplication and maintenance burden<br>
 * </p>
 */
public final class ControlFlowChainGenerator extends AbstractGenerator
    implements Function<ControlFlowChainDetails, List<IRInstr>> {

  private final Function<ExprProcessingDetails, List<IRInstr>> rawExprProcessor;
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final CallDetailsForIsTrue callDetailsForIsTrue = new CallDetailsForIsTrue();

  // Helper classes for evaluation patterns
  private final BooleanFalseEvaluationCreator booleanFalseEvaluationCreator;
  private final IsSetEvaluationCreator isSetEvaluationCreator;
  private final BooleanNotEvaluationCreator booleanNotEvaluationCreator;

  public ControlFlowChainGenerator(final IRGenerationContext stackContext,
                                   final VariableMemoryManagement variableMemoryManagement,
                                   final Function<ExprProcessingDetails, List<IRInstr>> rawExprProcessor) {
    super(stackContext);
    this.rawExprProcessor = rawExprProcessor;

    final var callDetailsForOfFalse = new CallDetailsForOfFalse();
    final var isSetCallDetailsCreator = new IsSetCallDetailsCreator();
    final var booleanNotCallDetailsCreator = new BooleanNotCallDetailsCreator();

    // Initialize helper classes with injected VariableMemoryManagement
    this.booleanFalseEvaluationCreator = new BooleanFalseEvaluationCreator(
        callDetailsForOfFalse, variableMemoryManagement);
    this.isSetEvaluationCreator = new IsSetEvaluationCreator(
        isSetCallDetailsCreator, variableMemoryManagement);
    this.booleanNotEvaluationCreator = new BooleanNotEvaluationCreator(
        booleanNotCallDetailsCreator, variableMemoryManagement);
  }

  @Override
  public List<IRInstr> apply(final ControlFlowChainDetails details) {
    final var instructions = new ArrayList<IRInstr>();

    // Add guard scope management if needed
    if (details.hasGuardScope()) {
      instructions.add(ScopeInstr.enter(details.guardScopeId(), details.debugInfo()));
      instructions.addAll(details.guardScopeSetup());
    }

    // Add shared condition scope management if needed
    if (details.hasSharedConditionScope()) {
      instructions.add(ScopeInstr.enter(details.conditionScopeId(), details.debugInfo()));
    }

    // Add the main CONTROL_FLOW_CHAIN instruction
    final var controlFlowChain = ControlFlowChainInstr.controlFlowChain(details);
    instructions.add(controlFlowChain);

    // Close shared condition scope if opened
    if (details.hasSharedConditionScope()) {
      instructions.add(ScopeInstr.exit(details.conditionScopeId(), details.debugInfo()));
    }

    // Close guard scope if opened
    if (details.hasGuardScope()) {
      instructions.add(ScopeInstr.exit(details.guardScopeId(), details.debugInfo()));
    }

    return instructions;
  }

  /**
   * Generate SWITCH_CHAIN_BLOCK for Question operator (?).
   * Converts "operand?" to a two-case chain: null check + _isSet() default.
   * Question operator is self-contained - no RETAIN/SCOPE_REGISTER needed on input.
   */
  public List<IRInstr> generateQuestionOperator(final ExprProcessingDetails exprDetails) {
    final var ctx = exprDetails.ctx();
    final var resultVariable = exprDetails.variableDetails().resultVariable();

    // Get debug information from expression symbol
    final var exprSymbol = getRecordedSymbolOrException(ctx);
    final var debugInfo = debugInfoCreator.apply(exprSymbol.getSourceToken());
    // STACK-BASED: Get scope ID from current stack frame 
    final var scopeId = stackContext.currentScopeId();

    // Generate operand evaluation WITHOUT memory management (question operator is self-contained)
    final var operandVariable = stackContext.generateTempName();
    final var operandVariableDetails = new VariableDetails(operandVariable, debugInfo);

    // Use raw processor to avoid automatic RETAIN/SCOPE_REGISTER on operand
    final var operandEvaluationInstructions = new ArrayList<>(
        rawExprProcessor.apply(new ExprProcessingDetails(ctx.expression(0), operandVariableDetails)));

    // Add explicit IS_NULL check directly on operand (no memory management needed)
    final var nullCheckCondition = stackContext.generateTempName();
    operandEvaluationInstructions.add(MemoryInstr.isNull(nullCheckCondition, operandVariable, debugInfo));

    final var nullCaseResult = stackContext.generateTempName();
    final var nullCaseDetails = new VariableDetails(nullCaseResult, debugInfo);
    final var nullCaseEvaluation = generateBooleanFalseEvaluation(nullCaseDetails);

    final var nullCheckCase = ConditionCaseDetails.createNullCheck(
        stackContext.currentScopeId(), // STACK-BASED: Get scope ID from current stack frame
        operandEvaluationInstructions,
        null, // No EK9 Boolean condition result for null check
        nullCheckCondition, // primitive boolean condition
        nullCaseEvaluation,
        nullCaseResult
    );

    // Create default case: else return operand._isSet()
    // Only the _isSet() result needs memory management, not the operand
    final var setCaseResult = stackContext.generateTempName();
    final var setCaseDetails = new VariableDetails(setCaseResult, debugInfo);
    // IMPORTANT: Use operand's type (from ctx.expression(0)), not result type (exprSymbol is Boolean)
    final var operandType = typeNameOrException.apply(getRecordedSymbolOrException(ctx.expression(0)));
    final var setCaseEvaluation = generateIsSetEvaluationNoOperandManagement(operandVariable,
        operandType, setCaseDetails);

    // Create the unified switch chain details
    final var controlFlowChainDetails = ControlFlowChainDetails.createQuestionOperator(
        resultVariable,
        List.of(nullCheckCase),
        setCaseEvaluation,
        setCaseResult,
        debugInfo,
        scopeId
    );

    return apply(controlFlowChainDetails);
  }

  /**
   * Generate SWITCH_CHAIN_BLOCK for Question operator applied to a variable.
   * Used by guarded assignment composition.
   */
  public List<IRInstr> generateQuestionOperatorForVariable(final ISymbol variableSymbol,
                                                           final String resultVariable,
                                                           final DebugInfo debugInfo) {
    // STACK-BASED: Get scope ID from current stack frame
    final var scopeId = stackContext.currentScopeId();

    // Direct null check on variable - no LOAD/RETAIN/SCOPE_REGISTER needed for null checking
    final var operandEvaluationInstructions = new ArrayList<IRInstr>();

    // Add explicit IS_NULL check directly on the variable
    final var nullCheckCondition = stackContext.generateTempName();
    operandEvaluationInstructions.add(
        MemoryInstr.isNull(nullCheckCondition, variableSymbol.getName(), debugInfo));

    final var nullCaseResult = stackContext.generateTempName();
    final var nullCaseDetails = new VariableDetails(nullCaseResult, debugInfo);
    final var nullCaseEvaluation = generateBooleanFalseEvaluation(nullCaseDetails);

    final var nullCheckCase = ConditionCaseDetails.createNullCheck(
        stackContext.currentScopeId(), // STACK-BASED: Get scope ID from current stack frame
        operandEvaluationInstructions,
        null, // No EK9 Boolean condition result
        nullCheckCondition,
        nullCaseEvaluation,
        nullCaseResult
    );

    // Create default case: else return variable._isSet()
    // Need to load variable for _isSet() method call (but not for null check)
    final var operandVariable = stackContext.generateTempName();
    final var operandDetails = new VariableDetails(operandVariable, debugInfo);

    final var setCaseResult = stackContext.generateTempName();
    final var setCaseDetails = new VariableDetails(setCaseResult, debugInfo);
    final var setCaseEvaluation = generateIsSetEvaluationForVariable(variableSymbol.getName(),
        operandVariable, operandDetails, typeNameOrException.apply(variableSymbol), setCaseDetails);

    // Create the unified switch chain details
    final var controlFlowChainDetails = ControlFlowChainDetails.createQuestionOperator(
        resultVariable,
        List.of(nullCheckCase),
        setCaseEvaluation,
        setCaseResult,
        debugInfo,
        scopeId
    );

    return apply(controlFlowChainDetails);
  }

  /**
   * Generate SWITCH_CHAIN_BLOCK for guarded assignment (:=?).
   * Composes question operator logic: assign only if LHS is null OR !LHS._isSet().
   */
  public List<IRInstr> generateGuardedAssignment(final ISymbol lhsSymbol,
                                                 final List<IRInstr> assignmentEvaluation,
                                                 final String assignmentResult,
                                                 final DebugInfo debugInfo) {

    // STACK-BASED: Get scope ID from current stack frame
    final var scopeId = stackContext.currentScopeId();

    // Generate question operator for condition: lhsSymbol?
    final var conditionResult = stackContext.generateTempName();
    final var questionOperatorInstructions = generateQuestionOperatorForVariable(
        lhsSymbol, conditionResult, debugInfo);

    // Invert the condition: assign when NOT set (when question operator returns false)
    final var invertedCondition = stackContext.generateTempName();
    final var invertedConditionDetails = new VariableDetails(invertedCondition, debugInfo);
    final var inversionInstructions = generateBooleanNotEvaluation(conditionResult, invertedConditionDetails);

    // Create the condition evaluation that includes question operator + inversion
    final var conditionEvaluationInstructions = new ArrayList<IRInstr>();
    conditionEvaluationInstructions.addAll(questionOperatorInstructions);
    conditionEvaluationInstructions.addAll(inversionInstructions);

    // Get primitive condition for backend optimization
    final var primitiveCondition = stackContext.generateTempName();

    conditionEvaluationInstructions.add(CallInstr.operator(
        new VariableDetails(primitiveCondition, debugInfo),
        callDetailsForIsTrue.apply(invertedCondition)
    ));

    final var assignmentCase = ConditionCaseDetails.createExpression(
        stackContext.currentScopeId(), // STACK-BASED: Get scope ID from current stack frame
        conditionEvaluationInstructions,
        invertedCondition, // EK9 Boolean result  
        primitiveCondition, // primitive boolean for backends
        assignmentEvaluation,
        assignmentResult
    );

    // No default case - guarded assignment either assigns or does nothing
    final var controlFlowChainDetails = new ControlFlowChainDetails(
        null, // No overall result
        "GUARDED_ASSIGNMENT", // Special chain type
        GuardVariableDetails.none(), // No guard variables (guarded assignment handles this internally)
        EvaluationVariableDetails.none(), // No evaluation variable
        ReturnVariableDetails.none(), // No return variable
        List.of(assignmentCase),
        DefaultCaseDetails.none(), // No default case
        null, // No enum optimization
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );

    return apply(controlFlowChainDetails);
  }

  /**
   * Generate evaluation instructions for Boolean(false).
   */
  private List<IRInstr> generateBooleanFalseEvaluation(final VariableDetails variableDetails) {
    return booleanFalseEvaluationCreator.apply(variableDetails);
  }

  /**
   * Generate evaluation instructions for variable._isSet() - loads variable first.
   * Note: LOAD does NOT get memory management - only the _isSet() result does.
   */
  private List<IRInstr> generateIsSetEvaluationForVariable(final String variableName,
                                                           final String operandVariable,
                                                           final VariableDetails operandDetails,
                                                           final String operandType,
                                                           final VariableDetails resultDetails) {

    // Load variable for _isSet() method call (no memory management on the LOAD)
    final var instructions = new ArrayList<IRInstr>();
    instructions.add(MemoryInstr.load(operandVariable, variableName, operandDetails.debugInfo()));

    // Call _isSet() with memory management on the result
    instructions.addAll(isSetEvaluationCreator.apply(operandVariable, operandType, resultDetails));

    return instructions;
  }

  /**
   * Generate evaluation instructions for operand._isSet() without operand memory management.
   * Used by question operator which is self-contained and manages its own inputs.
   */
  private List<IRInstr> generateIsSetEvaluationNoOperandManagement(final String operandVariable,
                                                                   final String operandType,
                                                                   final VariableDetails resultDetails) {
    return isSetEvaluationCreator.apply(operandVariable, operandType, resultDetails);
  }

  /**
   * Generate evaluation instructions for boolean._not().
   */
  private List<IRInstr> generateBooleanNotEvaluation(final String booleanVariable,
                                                     final VariableDetails resultDetails) {
    return booleanNotEvaluationCreator.apply(booleanVariable, resultDetails);
  }
}