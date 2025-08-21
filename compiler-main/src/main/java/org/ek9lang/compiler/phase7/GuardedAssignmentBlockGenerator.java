package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.DebugInfo;
import org.ek9lang.compiler.ir.GuardedAssignmentBlockInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.QuestionOperatorInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRContext;
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
  private final AssignExpressionToSymbol assignExpressionToSymbol;
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();

  public GuardedAssignmentBlockGenerator(final IRContext context,
                                         final AssignExpressionToSymbol assignExpressionToSymbol) {
    this.context = context;
    this.debugInfoCreator = new DebugInfoCreator(context);
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

    // Step 1: Generate condition evaluation (should assignment occur?)
    final var conditionResult = context.generateTempName();
    final var conditionEvaluationInstructions =
        generateConditionEvaluation(lhsSymbol, conditionResult, scopeId, debugInfo);

    // Step 2: Generate assignment evaluation instructions
    final var assignmentResult = context.generateTempName();
    final var assignmentEvaluationInstructions = new ArrayList<>(
        assignExpressionToSymbol.apply(lhsSymbol, assignmentExpression));

    // Step 3: Create guarded assignment block
    final var guardedAssignmentOperation = GuardedAssignmentBlockInstr.guardedAssignmentBlock(
        assignmentResult,
        conditionEvaluationInstructions,
        conditionResult,
        assignmentEvaluationInstructions,
        assignmentResult,
        scopeId,
        debugInfo
    );

    return List.of(guardedAssignmentOperation);
  }

  /**
   * Generate condition evaluation instructions.
   * Returns Boolean(true) if assignment should occur (LHS is null OR LHS._isSet() == false).
   */
  private List<IRInstr> generateConditionEvaluation(final ISymbol lhsSymbol,
                                                    final String conditionResult,
                                                    final String scopeId,
                                                    final DebugInfo debugInfo) {
    final var instructions = new ArrayList<IRInstr>();
    final var lhsVariableName = lhsSymbol.getName();

    // Load LHS for checking
    final var lhsTemp = context.generateTempName();
    instructions.add(MemoryInstr.load(lhsTemp, lhsVariableName, debugInfo));

    // Create QUESTION_BLOCK to handle null-safety logic
    // This will return true if lhs._isSet() (value is set), false if null/unset
    final var questionResult = context.generateTempName();

    // Operand evaluation with explicit IS_NULL check
    final var operandEvaluationInstructions = new ArrayList<IRInstr>();

    // Add explicit IS_NULL check for semantic clarity
    final var nullCheckCondition = context.generateTempName();
    operandEvaluationInstructions.add(MemoryInstr.isNull(nullCheckCondition, lhsTemp, debugInfo));

    // Null case: return Boolean(false) (unset, should assign)
    final var nullCaseResult = context.generateTempName();
    final var nullCaseEvaluationInstructions = new ArrayList<IRInstr>();
    final var falseCallDetails = new CallDetails("org.ek9.lang::Boolean", "org.ek9.lang::Boolean",
        "_ofFalse", List.of(), "org.ek9.lang::Boolean", List.of());
    nullCaseEvaluationInstructions.add(CallInstr.call(nullCaseResult, debugInfo, falseCallDetails));
    nullCaseEvaluationInstructions.add(MemoryInstr.retain(nullCaseResult, debugInfo));
    nullCaseEvaluationInstructions.add(ScopeInstr.register(nullCaseResult, scopeId, debugInfo));

    // Set case: call _isSet() method
    final var setCaseResult = context.generateTempName();
    final var setCaseEvaluationInstructions = new ArrayList<IRInstr>();
    final var typeName = typeNameOrException.apply(lhsSymbol);
    final var isSetCallDetails = new CallDetails(lhsTemp, typeName,
        "_isSet", List.of(), "org.ek9.lang::Boolean", List.of());
    setCaseEvaluationInstructions.add(CallInstr.call(setCaseResult, debugInfo, isSetCallDetails));
    setCaseEvaluationInstructions.add(MemoryInstr.retain(setCaseResult, debugInfo));
    setCaseEvaluationInstructions.add(ScopeInstr.register(setCaseResult, scopeId, debugInfo));

    // Create the QUESTION_BLOCK
    final var questionBlock = QuestionOperatorInstr.questionBlock(
        questionResult,
        operandEvaluationInstructions,
        lhsTemp,
        nullCheckCondition,
        nullCaseEvaluationInstructions,
        nullCaseResult,
        setCaseEvaluationInstructions,
        setCaseResult,
        scopeId,
        debugInfo
    );
    instructions.add(questionBlock);

    // Invert the question result: if _isSet() is true (set), we want false (don't assign)
    // if _isSet() is false (unset), we want true (do assign)
    final var notCallDetails = new CallDetails(questionResult, "org.ek9.lang::Boolean",
        "_not", List.of(), "org.ek9.lang::Boolean", List.of());
    instructions.add(CallInstr.call(conditionResult, debugInfo, notCallDetails));
    instructions.add(MemoryInstr.retain(conditionResult, debugInfo));
    instructions.add(ScopeInstr.register(conditionResult, scopeId, debugInfo));

    return instructions;
  }
}