package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
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
import org.ek9lang.compiler.phase7.calls.CallDetailsForIsTrue;
import org.ek9lang.compiler.phase7.calls.CallDetailsForOfFalse;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRInstrToList;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.support.EK9TypeNames;
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
  private final CallDetailsForOfFalse callDetailsForOfFalse = new CallDetailsForOfFalse();
  private final IRInstrToList irInstrToList = new IRInstrToList();
  private final VariableMemoryManagement variableMemoryManagement;
  private final CallDetailsForIsTrue callDetailsForIsTrue = new CallDetailsForIsTrue();
  private final OperatorMap operatorMap = new OperatorMap();

  public ControlFlowChainGenerator(final IRGenerationContext stackContext,
                                   final Function<ExprProcessingDetails, List<IRInstr>> rawExprProcessor) {
    super(stackContext);
    this.rawExprProcessor = rawExprProcessor;
    this.variableMemoryManagement = new VariableMemoryManagement(stackContext);
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
    final var operandBasicDetails = new BasicDetails(debugInfo);

    // Generate operand evaluation WITHOUT memory management (question operator is self-contained)
    final var operandVariable = stackContext.generateTempName();
    final var operandVariableDetails = new VariableDetails(operandVariable, operandBasicDetails);

    // Use raw processor to avoid automatic RETAIN/SCOPE_REGISTER on operand
    final var operandEvaluationInstructions = new ArrayList<>(
        rawExprProcessor.apply(new ExprProcessingDetails(ctx.expression(0), operandVariableDetails)));

    // Add explicit IS_NULL check directly on operand (no memory management needed)
    final var nullCheckCondition = stackContext.generateTempName();
    operandEvaluationInstructions.add(MemoryInstr.isNull(nullCheckCondition, operandVariable, debugInfo));

    final var nullCaseResult = stackContext.generateTempName();
    final var nullCaseDetails = new VariableDetails(nullCaseResult, operandBasicDetails);
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
    final var setCaseDetails = new VariableDetails(setCaseResult, operandBasicDetails);
    final var setCaseEvaluation = generateIsSetEvaluationNoOperandManagement(operandVariable,
        typeNameOrException.apply(exprSymbol), setCaseDetails);

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
                                                           final BasicDetails basicDetails) {
    // STACK-BASED: Get scope ID from current stack frame
    final var scopeId = stackContext.currentScopeId();

    // Direct null check on variable - no LOAD/RETAIN/SCOPE_REGISTER needed for null checking
    final var operandEvaluationInstructions = new ArrayList<IRInstr>();

    // Add explicit IS_NULL check directly on the variable
    final var nullCheckCondition = stackContext.generateTempName();
    operandEvaluationInstructions.add(
        MemoryInstr.isNull(nullCheckCondition, variableSymbol.getName(), basicDetails.debugInfo()));

    final var nullCaseResult = stackContext.generateTempName();
    final var nullCaseDetails = new VariableDetails(nullCaseResult, basicDetails);
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
    final var operandDetails = new VariableDetails(operandVariable, basicDetails);

    final var setCaseResult = stackContext.generateTempName();
    final var setCaseDetails = new VariableDetails(setCaseResult, basicDetails);
    final var setCaseEvaluation = generateIsSetEvaluationForVariable(variableSymbol.getName(),
        operandVariable, operandDetails, typeNameOrException.apply(variableSymbol), setCaseDetails);

    // Create the unified switch chain details
    final var controlFlowChainDetails = ControlFlowChainDetails.createQuestionOperator(
        resultVariable,
        List.of(nullCheckCase),
        setCaseEvaluation,
        setCaseResult,
        basicDetails.debugInfo(),
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
                                                 final BasicDetails basicDetails) {

    // STACK-BASED: Get scope ID from current stack frame
    final var scopeId = stackContext.currentScopeId();

    // Generate question operator for condition: lhsSymbol?
    final var conditionResult = stackContext.generateTempName();
    final var questionOperatorInstructions = generateQuestionOperatorForVariable(
        lhsSymbol, conditionResult, basicDetails);

    // Invert the condition: assign when NOT set (when question operator returns false)
    final var invertedCondition = stackContext.generateTempName();
    final var invertedConditionDetails = new VariableDetails(invertedCondition, basicDetails);
    final var inversionInstructions = generateBooleanNotEvaluation(conditionResult, invertedConditionDetails);

    // Create the condition evaluation that includes question operator + inversion
    final var conditionEvaluationInstructions = new ArrayList<IRInstr>();
    conditionEvaluationInstructions.addAll(questionOperatorInstructions);
    conditionEvaluationInstructions.addAll(inversionInstructions);

    // Get primitive condition for backend optimization
    final var primitiveCondition = stackContext.generateTempName();

    conditionEvaluationInstructions.add(CallInstr.operator(
        new VariableDetails(primitiveCondition, basicDetails),
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
        basicDetails.debugInfo(),
        scopeId
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
    // For now use default metadata since type resolution from string is complex
    final var isSetMetaData = new CallMetaDataDetails(true, 0);
    final var methodName = operatorMap.getForward("?");
    final var isSetCallDetails = new CallDetails(operandVariable, operandType,
        methodName, List.of(), EK9TypeNames.EK9_BOOLEAN, List.of(), isSetMetaData);
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
    // For now use default metadata since type resolution from string is complex
    final var isSetMetaData = new CallMetaDataDetails(true, 0);

    final var methodName = operatorMap.getForward("?");
    final var isSetCallDetails = new CallDetails(operandVariable, operandType,
        methodName, List.of(), EK9TypeNames.EK9_BOOLEAN, List.of(), isSetMetaData);

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
    final var booleanType = EK9TypeNames.EK9_BOOLEAN;

    // Create metadata for _not operator call on Boolean
    final var notMetaData = new CallMetaDataDetails(true, 0);
    final var methodName = operatorMap.getForward("~");
    final var notCallDetails = new CallDetails(booleanVariable, booleanType,
        methodName, List.of(), booleanType, List.of(), notMetaData);

    final var instructions = irInstrToList.apply(() -> CallInstr.operator(variableDetails, notCallDetails));
    variableMemoryManagement.apply(() -> instructions, variableDetails);
    return instructions;
  }
}