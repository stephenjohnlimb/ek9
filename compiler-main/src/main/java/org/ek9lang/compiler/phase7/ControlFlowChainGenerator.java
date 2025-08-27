package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.ConditionCase;
import org.ek9lang.compiler.ir.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.ControlFlowChainInstr;
import org.ek9lang.compiler.ir.DefaultCaseDetails;
import org.ek9lang.compiler.ir.EvaluationVariableDetails;
import org.ek9lang.compiler.ir.GuardVariableDetails;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.ReturnVariableDetails;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.CallDetailsForOfFalse;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.IRInstrToList;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Unified generator for all EK9 control flow constructs using CONTROL_FLOW_CHAIN.
 * <p>
 * This generator replaces multiple specialized generators:
 * - QuestionBlockGenerator → generateQuestionOperator()
 * - GuardedAssignmentBlockGenerator → generateGuardedAssignment()
 * - Future: If/else and switch statement generators
 * </p>
 * <p>
 * Key architectural benefits:
 * - Single source of truth for all control flow logic
 * - Consistent memory management patterns across all constructs
 * - Unified optimization metadata for backend code generation
 * - Reduced code duplication and maintenance burden
 * </p>
 */
public final class ControlFlowChainGenerator implements Function<ControlFlowChainDetails, List<IRInstr>> {

  private final IRContext context;
  private final DebugInfoCreator debugInfoCreator;
  private final Function<ExprProcessingDetails, List<IRInstr>> rawExprProcessor;
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final CallDetailsForOfFalse callDetailsForOfFalse = new CallDetailsForOfFalse();
  private final IRInstrToList irInstrToList = new IRInstrToList();
  private final VariableMemoryManagement variableMemoryManagement = new VariableMemoryManagement();

  public ControlFlowChainGenerator(final IRContext context,
                                   final Function<ExprProcessingDetails, List<IRInstr>> rawExprProcessor) {
    this.context = context;
    this.debugInfoCreator = new DebugInfoCreator(context);
    this.rawExprProcessor = rawExprProcessor;
  }

  @Override
  public List<IRInstr> apply(final ControlFlowChainDetails details) {
    final var instructions = new ArrayList<IRInstr>();

    // Add guard scope management if needed
    if (details.hasGuardScope()) {
      instructions.add(ScopeInstr.enter(details.guardScopeId(), details.basicDetails().debugInfo()));
      instructions.addAll(details.guardScopeSetup());
    }

    // Add shared condition scope management if needed  
    if (details.hasSharedConditionScope()) {
      instructions.add(ScopeInstr.enter(details.conditionScopeId(), details.basicDetails().debugInfo()));
    }

    // Add the main CONTROL_FLOW_CHAIN instruction
    final var controlFlowChain = ControlFlowChainInstr.controlFlowChain(details);
    instructions.add(controlFlowChain);

    // Close shared condition scope if opened
    if (details.hasSharedConditionScope()) {
      instructions.add(ScopeInstr.exit(details.conditionScopeId(), details.basicDetails().debugInfo()));
    }

    // Close guard scope if opened
    if (details.hasGuardScope()) {
      instructions.add(ScopeInstr.exit(details.guardScopeId(), details.basicDetails().debugInfo()));
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
    final var basicDetails = exprDetails.variableDetails().basicDetails();

    // Get debug information from expression symbol
    final var exprSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    final var debugInfo = debugInfoCreator.apply(exprSymbol.getSourceToken());
    final var operandBasicDetails = new BasicDetails(basicDetails.scopeId(), debugInfo);

    // Generate operand evaluation WITHOUT memory management (question operator is self-contained)
    final var operandVariable = context.generateTempName();
    final var operandVariableDetails = new VariableDetails(operandVariable, operandBasicDetails);

    // Use raw processor to avoid automatic RETAIN/SCOPE_REGISTER on operand
    final var operandEvaluationInstructions = new ArrayList<>(
        rawExprProcessor.apply(new ExprProcessingDetails(ctx.expression(0), operandVariableDetails)));

    // Add explicit IS_NULL check directly on operand (no memory management needed)
    final var nullCheckCondition = context.generateTempName();
    operandEvaluationInstructions.add(MemoryInstr.isNull(nullCheckCondition, operandVariable, debugInfo));

    final var nullCaseResult = context.generateTempName();
    final var nullCaseDetails = new VariableDetails(nullCaseResult, operandBasicDetails);
    final var nullCaseEvaluation = generateBooleanFalseEvaluation(nullCaseDetails);

    final var nullCheckCase = ConditionCase.createNullCheck(
        basicDetails.scopeId(), // case scope same as main scope for Question operator
        operandEvaluationInstructions,
        null, // No EK9 Boolean condition result for null check
        nullCheckCondition, // primitive boolean condition
        nullCaseEvaluation,
        nullCaseResult
    );

    // Create default case: else return operand._isSet()
    // Only the _isSet() result needs memory management, not the operand
    final var setCaseResult = context.generateTempName();
    final var setCaseDetails = new VariableDetails(setCaseResult, operandBasicDetails);
    final var setCaseEvaluation = generateIsSetEvaluationNoOperandManagement(operandVariable,
        typeNameOrException.apply(exprSymbol), setCaseDetails);

    // Create the unified switch chain details
    final var controlFlowChainDetails = ControlFlowChainDetails.createQuestionOperator(
        resultVariable,
        List.of(nullCheckCase),
        setCaseEvaluation,
        setCaseResult,
        basicDetails
    );

    return apply(controlFlowChainDetails);
  }

  /**
   * Generate SWITCH_CHAIN_BLOCK for Question operator applied to a variable.
   * Used by guarded assignment composition.
   */
  public List<IRInstr> generateQuestionOperatorForVariable(final ISymbol variableSymbol,
                                                           final String resultVariable,
                                                           final BasicDetails basicDetails) {
    // Direct null check on variable - no LOAD/RETAIN/SCOPE_REGISTER needed for null checking
    final var operandEvaluationInstructions = new ArrayList<IRInstr>();

    // Add explicit IS_NULL check directly on the variable
    final var nullCheckCondition = context.generateTempName();
    operandEvaluationInstructions.add(
        MemoryInstr.isNull(nullCheckCondition, variableSymbol.getName(), basicDetails.debugInfo()));

    final var nullCaseResult = context.generateTempName();
    final var nullCaseDetails = new VariableDetails(nullCaseResult, basicDetails);
    final var nullCaseEvaluation = generateBooleanFalseEvaluation(nullCaseDetails);

    final var nullCheckCase = ConditionCase.createNullCheck(
        basicDetails.scopeId(),
        operandEvaluationInstructions,
        null, // No EK9 Boolean condition result
        nullCheckCondition,
        nullCaseEvaluation,
        nullCaseResult
    );

    // Create default case: else return variable._isSet()
    // Need to load variable for _isSet() method call (but not for null check)
    final var operandVariable = context.generateTempName();
    final var operandDetails = new VariableDetails(operandVariable, basicDetails);

    final var setCaseResult = context.generateTempName();
    final var setCaseDetails = new VariableDetails(setCaseResult, basicDetails);
    final var setCaseEvaluation = generateIsSetEvaluationForVariable(variableSymbol.getName(),
        operandVariable, operandDetails, typeNameOrException.apply(variableSymbol), setCaseDetails);

    // Create the unified switch chain details
    final var controlFlowChainDetails = ControlFlowChainDetails.createQuestionOperator(
        resultVariable,
        List.of(nullCheckCase),
        setCaseEvaluation,
        setCaseResult,
        basicDetails
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
                                                 final BasicDetails basicDetails) {

    // Generate question operator for condition: lhsSymbol?
    final var conditionResult = context.generateTempName();
    final var questionOperatorInstructions = generateQuestionOperatorForVariable(
        lhsSymbol, conditionResult, basicDetails);

    // Invert the condition: assign when NOT set (when question operator returns false)
    final var invertedCondition = context.generateTempName();
    final var invertedConditionDetails = new VariableDetails(invertedCondition, basicDetails);
    final var inversionInstructions = generateBooleanNotEvaluation(conditionResult, invertedConditionDetails);

    // Create the condition evaluation that includes question operator + inversion
    final var conditionEvaluationInstructions = new ArrayList<IRInstr>();
    conditionEvaluationInstructions.addAll(questionOperatorInstructions);
    conditionEvaluationInstructions.addAll(inversionInstructions);

    // Get primitive condition for backend optimization
    final var primitiveCondition = context.generateTempName();
    conditionEvaluationInstructions.add(CallInstr.operator(
        new VariableDetails(primitiveCondition, basicDetails),
        new CallDetails(invertedCondition, "org.ek9.lang::Boolean", "_true",
            List.of(), "boolean", List.of())
    ));

    final var assignmentCase = ConditionCase.createExpression(
        basicDetails.scopeId(),
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
        basicDetails
    );

    return apply(controlFlowChainDetails);
  }

  /**
   * Generate evaluation instructions for Boolean(false).
   */
  private List<IRInstr> generateBooleanFalseEvaluation(final VariableDetails variableDetails) {
    final var instructions = irInstrToList
        .apply(() -> CallInstr.callStatic(variableDetails, callDetailsForOfFalse.get()));

    variableMemoryManagement.apply(() -> instructions, variableDetails);
    return instructions;
  }

  /**
   * Generate evaluation instructions for variable._isSet() - loads variable first.
   */
  private List<IRInstr> generateIsSetEvaluationForVariable(final String variableName,
                                                           final String operandVariable,
                                                           final VariableDetails operandDetails,
                                                           final String operandType,
                                                           final VariableDetails resultDetails) {

    // Load variable for _isSet() method call
    final var loadInstructions = irInstrToList
        .apply(() -> MemoryInstr.load(operandVariable, variableName, operandDetails.basicDetails().debugInfo()));
    final var instructions = new ArrayList<>(loadInstructions);
    variableMemoryManagement.apply(() -> loadInstructions, operandDetails);

    // Call _isSet() on loaded variable
    final var isSetCallDetails = new CallDetails(operandVariable, operandType,
        "_isSet", List.of(), "org.ek9.lang::Boolean", List.of());
    final var callInstructions = irInstrToList.apply(() -> CallInstr.operator(resultDetails, isSetCallDetails));
    instructions.addAll(callInstructions);
    variableMemoryManagement.apply(() -> callInstructions, resultDetails);

    return instructions;
  }

  /**
   * Generate evaluation instructions for operand._isSet() without operand memory management.
   * Used by question operator which is self-contained and manages its own inputs.
   */
  private List<IRInstr> generateIsSetEvaluationNoOperandManagement(final String operandVariable,
                                                                   final String operandType,
                                                                   final VariableDetails resultDetails) {
    final var isSetCallDetails = new CallDetails(operandVariable, operandType,
        "_isSet", List.of(), "org.ek9.lang::Boolean", List.of());

    // Only manage memory for the _isSet() result, not the operand
    final var instructions = irInstrToList.apply(() -> CallInstr.operator(resultDetails, isSetCallDetails));
    variableMemoryManagement.apply(() -> instructions, resultDetails);
    return instructions;
  }

  /**
   * Generate evaluation instructions for boolean._not().
   */
  private List<IRInstr> generateBooleanNotEvaluation(final String booleanVariable,
                                                     final VariableDetails variableDetails) {
    final var notCallDetails = new CallDetails(booleanVariable, "org.ek9.lang::Boolean",
        "_not", List.of(), "org.ek9.lang::Boolean", List.of());

    final var instructions = irInstrToList.apply(() -> CallInstr.operator(variableDetails, notCallDetails));
    variableMemoryManagement.apply(() -> instructions, variableDetails);
    return instructions;
  }
}