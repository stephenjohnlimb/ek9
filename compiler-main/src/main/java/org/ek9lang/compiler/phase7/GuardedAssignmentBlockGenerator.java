package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.GuardedAssignmentBlockInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.CallDetailsOnBoolean;
import org.ek9lang.compiler.phase7.support.ConditionalEvaluation;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.OperandEvaluation;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Simplified guarded assignment generator using GUARDED_ASSIGNMENT_BLOCK.
 * <p>
 * This generator creates a declarative guarded assignment block that uses
 * composition to reuse existing proven patterns for null-safety and assignment.
 * </p>
 */
final class GuardedAssignmentBlockGenerator
    implements Function<GuardedAssignmentGenerator.GuardedAssignmentDetails, List<IRInstr>> {

  private final IRContext context;
  private final DebugInfoCreator debugInfoCreator;
  private final QuestionBlockGenerator questionBlockGenerator;
  private final AssignExpressionToSymbol assignExpressionToSymbol;
  private final CallDetailsOnBoolean callDetailsOnBoolean = new CallDetailsOnBoolean();
  private final VariableMemoryManagement variableMemoryManagement = new VariableMemoryManagement();

  public GuardedAssignmentBlockGenerator(final IRContext context,
                                         final QuestionBlockGenerator questionBlockGenerator,
                                         final AssignExpressionToSymbol assignExpressionToSymbol) {
    this.context = context;
    this.debugInfoCreator = new DebugInfoCreator(context);
    this.questionBlockGenerator = questionBlockGenerator;
    this.assignExpressionToSymbol = assignExpressionToSymbol;
  }

  @Override
  public List<IRInstr> apply(final GuardedAssignmentGenerator.GuardedAssignmentDetails details) {
    final var lhsSymbol = details.lhsSymbol();
    final var assignmentExpression = details.assignmentExpression();
    final var scopeId = details.scopeId();

    // Get debug information
    final var exprSymbol = context.getParsedModule().getRecordedSymbol(assignmentExpression);
    final var debugInfo = debugInfoCreator.apply(exprSymbol.getSourceToken());

    final var basicDetails = new BasicDetails(scopeId, debugInfo);
    // Step 1: Generate condition evaluation (should assignment occur?)
    final var conditionResult = context.generateTempName();
    final var variableDetails = new VariableDetails(conditionResult, basicDetails);

    final var conditionEvaluationInstructions = variableMemoryManagement
        .apply(() -> generateConditionEvaluation(lhsSymbol, conditionResult, basicDetails), variableDetails);

    // Step 2: Generate assignment evaluation instructions
    final var assignmentEvaluationInstructions = new ArrayList<>(
        assignExpressionToSymbol.apply(lhsSymbol, assignmentExpression));

    // Step 3: Create record components for structured data
    final var conditionalEvaluation = new ConditionalEvaluation(conditionEvaluationInstructions, conditionResult);
    final var assignmentEvaluation = new OperandEvaluation(assignmentEvaluationInstructions, null);

    // Create guarded assignment block with structured records
    final var guardedAssignmentOperation = GuardedAssignmentBlockInstr.guardedAssignmentBlock(
        conditionalEvaluation,
        assignmentEvaluation,
        basicDetails
    );

    return List.of(guardedAssignmentOperation);
  }

  /**
   * Generate condition evaluation using QuestionBlockGenerator composition.
   * Returns Boolean(true) if assignment should occur (LHS is null OR LHS._isSet() == false).
   * Uses composition by delegating QUESTION_BLOCK creation to QuestionBlockGenerator's
   * variable-based question evaluation method.
   */
  private List<IRInstr> generateConditionEvaluation(final ISymbol lhsSymbol,
                                                    final String conditionResult,
                                                    final BasicDetails basicDetails) {

    // Step 1: Use QuestionBlockGenerator for consistent null-safety logic
    final var questionResult = context.generateTempName();
    final var variableDetails = new VariableDetails(questionResult, basicDetails);
    final var questionInstructions = questionBlockGenerator.createQuestionBlockForVariable(
        lhsSymbol, questionResult, basicDetails);
    variableMemoryManagement.apply(() -> questionInstructions, variableDetails);

    // Step 2: Invert the question result for assignment condition  
    // Question returns true if variable is set, but we want to assign when unset
    final var notCallDetails = callDetailsOnBoolean.apply(questionResult, "_not");
    questionInstructions.add(CallInstr.call(conditionResult, basicDetails.debugInfo(), notCallDetails));

    return questionInstructions;
  }
}