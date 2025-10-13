package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.GuardedAssignmentDetails;

/**
 * Guarded assignment generator using unified CONTROL_FLOW_CHAIN approach.
 * <p>
 * This generator has been migrated to use the unified control flow:
 * 1. Delegates to injected ControlFlowChainGenerator for actual IR generation
 * 2. Maintains backward compatibility with existing call sites
 * 3. Uses CONTROL_FLOW_CHAIN with GUARDED_ASSIGNMENT chain type
 * </p>
 * <p>
 * Guarded assignment becomes a CONTROL_FLOW_CHAIN with condition:
 * if (!lhsSymbol?) then assign else do nothing
 * </p>
 * <p>
 * PHASE 7 REFACTORING COMPLETE: Now accepts injected ControlFlowChainGenerator for maximum object reuse.
 * </p>
 */
final class GuardedAssignmentBlockGenerator extends AbstractGenerator
    implements Function<GuardedAssignmentDetails, List<IRInstr>> {

  private final ControlFlowChainGenerator controlFlowChainGenerator;
  private final AssignExpressionToSymbol assignExpressionToSymbol;

  public GuardedAssignmentBlockGenerator(final IRGenerationContext stackContext,
                                         final ControlFlowChainGenerator controlFlowChainGenerator,
                                         final AssignExpressionToSymbol assignExpressionToSymbol) {
    super(stackContext);
    this.controlFlowChainGenerator = controlFlowChainGenerator;
    this.assignExpressionToSymbol = assignExpressionToSymbol;
  }

  @Override
  public List<IRInstr> apply(final GuardedAssignmentDetails details) {
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