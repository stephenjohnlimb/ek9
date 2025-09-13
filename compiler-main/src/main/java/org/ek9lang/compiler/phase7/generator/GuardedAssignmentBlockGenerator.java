package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;

/**
 * Guarded assignment generator using unified CONTROL_FLOW_CHAIN approach.
 * <p>
 * This generator has been migrated to use the unified control flow:
 * 1. Delegates to ControlFlowChainGenerator for actual IR generation
 * 2. Maintains backward compatibility with existing call sites
 * 3. Uses CONTROL_FLOW_CHAIN with GUARDED_ASSIGNMENT chain type
 * </p>
 * <p>
 * Guarded assignment becomes a CONTROL_FLOW_CHAIN with condition:
 * if (!lhsSymbol?) then assign else do nothing
 * </p>
 */
final class GuardedAssignmentBlockGenerator extends AbstractGenerator
    implements Function<GuardedAssignmentGenerator.GuardedAssignmentDetails, List<IRInstr>> {

  private final ControlFlowChainGenerator controlFlowChainGenerator;
  private final AssignExpressionToSymbol assignExpressionToSymbol;

  public GuardedAssignmentBlockGenerator(final IRGenerationContext stackContext,
                                         final AssignExpressionToSymbol assignExpressionToSymbol) {
    super(stackContext);
    this.assignExpressionToSymbol = assignExpressionToSymbol;

    // Extract RecordExprProcessing from the QuestionBlockGenerator's SwitchChainBlockGenerator
    // For now, create a new instance - in future this could be optimized to share instances
    // Pass null for both RecordExprProcessing and rawExprProcessor as this generator doesn't use expression processing
    this.controlFlowChainGenerator = new ControlFlowChainGenerator(stackContext, null);
  }

  @Override
  public List<IRInstr> apply(final GuardedAssignmentGenerator.GuardedAssignmentDetails details) {
    final var lhsSymbol = details.lhsSymbol();
    final var assignmentExpression = details.assignmentExpression();

    // Get debug information
    final var exprSymbol = getRecordedSymbolOrException(assignmentExpression);
    final var debugInfo = debugInfoCreator.apply(exprSymbol.getSourceToken());

    // Generate assignment evaluation instructions
    final var assignmentEvaluationInstructions = new ArrayList<>(
        assignExpressionToSymbol.apply(lhsSymbol, assignmentExpression));

    // Delegate to unified SwitchChainBlockGenerator
    return controlFlowChainGenerator.generateGuardedAssignment(
        lhsSymbol,
        assignmentEvaluationInstructions,
        null, // No specific assignment result variable
        debugInfo
    );
  }

}