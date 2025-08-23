package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.QuestionDetails;
import org.ek9lang.compiler.ir.QuestionOperatorInstr;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.CallDetailsForOfFalse;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.IRInstrToList;
import org.ek9lang.compiler.phase7.support.OperandEvaluation;
import org.ek9lang.compiler.phase7.support.RecordExprProcessing;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Generates IR instructions for question operator (?) using QUESTION_BLOCK pattern.
 * <p>
 * This generator creates a declarative question operator block containing:
 * 1. Operand evaluation instructions (evaluate the expression that ? is applied to)
 * 2. Null case evaluation instructions (create Boolean(false) for null operands)
 * 3. Set case evaluation instructions (call _isSet() method for non-null operands)
 * 4. All memory management for both paths
 * </p>
 * <p>
 * Backends can choose between branching and conditional selection strategies
 * based on the usage context and target-specific optimizations.
 * </p>
 */
public final class QuestionBlockGenerator implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final IRContext context;
  private final DebugInfoCreator debugInfoCreator;
  private final RecordExprProcessing recordExprProcessing;
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final CallDetailsForOfFalse callDetailsForOfFalse = new CallDetailsForOfFalse();
  private final IRInstrToList irInstrToList = new IRInstrToList();
  private final VariableMemoryManagement variableMemoryManagement = new VariableMemoryManagement();

  public QuestionBlockGenerator(final IRContext context,
                                final RecordExprProcessing recordExprProcessing) {
    this.context = context;
    this.debugInfoCreator = new DebugInfoCreator(context);
    this.recordExprProcessing = recordExprProcessing;
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {
    final var ctx = details.ctx();
    final var exprResult = details.variableDetails().resultVariable();
    final var scopeId = details.variableDetails().basicDetails().scopeId();

    // Get debug information
    final var exprSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    final var debugInfo = debugInfoCreator.apply(exprSymbol.getSourceToken());

    final var basicDetails = new BasicDetails(scopeId, debugInfo);
    // Generate operand evaluation instructions with explicit IS_NULL check
    final var targetVariable = context.generateTempName();
    final var operandEvaluationInstructions = new ArrayList<>(
        recordExprProcessing.apply(new ExprProcessingDetails(ctx.expression(0),
            new VariableDetails(targetVariable, basicDetails))));

    // Add explicit IS_NULL check for semantic clarity
    final var nullCheckCondition = context.generateTempName();
    operandEvaluationInstructions.add(MemoryInstr.isNull(nullCheckCondition, targetVariable, debugInfo));

    final var typeName = typeNameOrException.apply(exprSymbol);

    return createQuestionBlock(exprResult, operandEvaluationInstructions, nullCheckCondition, targetVariable,
        typeName, basicDetails);
  }

  /**
   * Create QUESTION_BLOCK for a variable symbol (used by guarded assignment composition).
   * This method enables composition by allowing other generators to reuse the core
   * QUESTION_BLOCK logic for variable-based null-safety checking without requiring
   * AST expression contexts.
   */
  public List<IRInstr> createQuestionBlockForVariable(final ISymbol variableSymbol,
                                                      final String resultName,
                                                      final BasicDetails basicDetails) {
    // Load the variable for checking
    final var resultVariable = context.generateTempName();
    final var variableDetails = new VariableDetails(resultVariable, basicDetails);

    final var instructions = irInstrToList
        .apply(() -> MemoryInstr.load(resultVariable, variableSymbol.getName(), basicDetails.debugInfo()));

    variableMemoryManagement.apply(() -> instructions, variableDetails);

    // Add explicit IS_NULL check for semantic clarity
    final var nullCheckCondition = context.generateTempName();
    instructions.add(MemoryInstr.isNull(nullCheckCondition, resultVariable, basicDetails.debugInfo()));

    final var typeName = typeNameOrException.apply(variableSymbol);

    return createQuestionBlock(resultName, instructions, nullCheckCondition, resultVariable,
        typeName, basicDetails);
  }

  /**
   * Core method to create QUESTION_BLOCK with all common logic.
   * Used by both expression-based and variable-based question operators.
   */
  private List<IRInstr> createQuestionBlock(final String resultName,
                                            final List<IRInstr> operandEvaluationInstructions,
                                            final String nullCheckCondition,
                                            final String targetObject,
                                            final String typeName,
                                            final BasicDetails basicDetails) {

    // Generate null case evaluation instructions (Boolean(false))
    final var nullCaseResult = context.generateTempName();
    final var nullCaseDetails = new VariableDetails(nullCaseResult, basicDetails);
    final var nullCaseEvaluationInstructions = generateNullCaseEvaluation(nullCaseDetails);
    variableMemoryManagement.apply(() -> nullCaseEvaluationInstructions, nullCaseDetails);

    // Generate set case evaluation instructions (call _isSet() method)
    final var setCaseResult = context.generateTempName();
    final var setCaseDetails = new VariableDetails(setCaseResult, basicDetails);
    final var setCaseEvaluationInstructions = generateSetCaseEvaluation(targetObject, typeName, setCaseDetails);
    variableMemoryManagement.apply(() -> setCaseEvaluationInstructions, setCaseDetails);

    // Create record components for structured data
    final var operandEvaluation = new OperandEvaluation(operandEvaluationInstructions, targetObject);
    final var nullCaseEvaluation = new OperandEvaluation(nullCaseEvaluationInstructions, nullCaseResult);
    final var setCaseEvaluation = new OperandEvaluation(setCaseEvaluationInstructions, setCaseResult);

    // Create question operator block with structured records
    final var questionOperation = QuestionOperatorInstr.questionBlock(
        new QuestionDetails(
            resultName,
            operandEvaluation,
            nullCheckCondition,
            nullCaseEvaluation,
            setCaseEvaluation,
            basicDetails)
    );

    final var rtn = new ArrayList<IRInstr>();
    rtn.add(questionOperation);
    return rtn;
  }

  /**
   * Generate null case evaluation instructions (Boolean(false)).
   */
  private List<IRInstr> generateNullCaseEvaluation(final VariableDetails variableDetails) {

    return irInstrToList
        .apply(() -> CallInstr.callStatic(variableDetails, callDetailsForOfFalse.get()));
  }

  /**
   * Generate set case evaluation instructions (call _isSet() method).
   */
  private List<IRInstr> generateSetCaseEvaluation(final String targetObject,
                                                  final String typeName,
                                                  final VariableDetails variableDetails) {

    final var isSetCallDetails = new CallDetails(targetObject, typeName,
        "_isSet", List.of(), "org.ek9.lang::Boolean", List.of());
    return irInstrToList.apply(() -> CallInstr.operator(variableDetails, isSetCallDetails));
  }
}