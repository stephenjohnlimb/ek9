package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.QuestionOperatorInstr;
import org.ek9lang.compiler.ir.ScopeInstr;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.RecordExprProcessing;

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
  private final OperatorMap operatorMap = new OperatorMap();
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();

  public QuestionBlockGenerator(final IRContext context,
                               final RecordExprProcessing recordExprProcessing) {
    this.context = context;
    this.debugInfoCreator = new DebugInfoCreator(context);
    this.recordExprProcessing = recordExprProcessing;
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {
    final var ctx = details.ctx();
    final var exprResult = details.exprResult();
    final var scopeId = details.scopeId();

    // Get debug information
    final var exprSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    final var debugInfo = debugInfoCreator.apply(exprSymbol.getSourceToken());

    // Generate operand evaluation instructions with explicit IS_NULL check
    final var operandTemp = context.generateTempName();
    final var operandEvaluationInstructions = new ArrayList<>(
        recordExprProcessing.apply(new ExprProcessingDetails(ctx.expression(0), operandTemp, scopeId, debugInfo)));
    
    // Add explicit IS_NULL check for semantic clarity
    final var nullCheckCondition = context.generateTempName();
    operandEvaluationInstructions.add(MemoryInstr.isNull(nullCheckCondition, operandTemp, debugInfo));

    // Generate null case evaluation instructions (Boolean(false))
    final var nullCaseResult = context.generateTempName();
    final var nullCaseEvaluationInstructions = new ArrayList<IRInstr>();
    final var falseCallDetails = new CallDetails("org.ek9.lang::Boolean", "org.ek9.lang::Boolean", 
        "_ofFalse", List.of(), "org.ek9.lang::Boolean", List.of());
    nullCaseEvaluationInstructions.add(CallInstr.call(nullCaseResult, debugInfo, falseCallDetails));
    nullCaseEvaluationInstructions.add(MemoryInstr.retain(nullCaseResult, debugInfo));
    nullCaseEvaluationInstructions.add(ScopeInstr.register(nullCaseResult, scopeId, debugInfo));

    // Generate set case evaluation instructions (call _isSet() method)
    final var setCaseResult = context.generateTempName();
    final var setCaseEvaluationInstructions = new ArrayList<IRInstr>();
    
    final var typeName = typeNameOrException.apply(exprSymbol);
    final var methodName = operatorMap.getForward(ctx.op.getText()); // Should be "_isSet"
    
    final var isSetCallDetails = new CallDetails(operandTemp, typeName,
        methodName, List.of(), "org.ek9.lang::Boolean", List.of());
    setCaseEvaluationInstructions.add(CallInstr.operator(setCaseResult, debugInfo, isSetCallDetails));
    setCaseEvaluationInstructions.add(MemoryInstr.retain(setCaseResult, debugInfo));
    setCaseEvaluationInstructions.add(ScopeInstr.register(setCaseResult, scopeId, debugInfo));

    // Create question operator block
    final var questionOperation = QuestionOperatorInstr.questionBlock(
        exprResult,
        operandEvaluationInstructions,
        operandTemp,
        nullCheckCondition,
        nullCaseEvaluationInstructions,
        nullCaseResult,
        setCaseEvaluationInstructions,
        setCaseResult,
        scopeId,
        debugInfo
    );

    return List.of(questionOperation);
  }
}