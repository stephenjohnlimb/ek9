package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRContext;

/**
 * Guarded assignment generator using unified SWITCH_CHAIN_BLOCK approach.
 * <p>
 * This generator has been migrated to use the unified control flow:
 * 1. Delegates to SwitchChainBlockGenerator for actual IR generation
 * 2. Maintains backward compatibility with existing call sites
 * 3. Uses SWITCH_CHAIN_BLOCK with GUARDED_ASSIGNMENT chain type
 * </p>
 * <p>
 * Guarded assignment becomes a SWITCH_CHAIN_BLOCK with condition:
 * if (!lhsSymbol?) then assign else do nothing
 * </p>
 */
final class GuardedAssignmentBlockGenerator
    implements Function<GuardedAssignmentGenerator.GuardedAssignmentDetails, List<IRInstr>> {

  private final SwitchChainBlockGenerator switchChainBlockGenerator;
  private final DebugInfoCreator debugInfoCreator;
  private final AssignExpressionToSymbol assignExpressionToSymbol;
  private final IRContext context;

  public GuardedAssignmentBlockGenerator(final IRContext context,
                                         final QuestionBlockGenerator questionBlockGenerator,
                                         final AssignExpressionToSymbol assignExpressionToSymbol) {
    this.context = context;
    this.debugInfoCreator = new DebugInfoCreator(context);
    this.assignExpressionToSymbol = assignExpressionToSymbol;

    // Extract RecordExprProcessing from the QuestionBlockGenerator's SwitchChainBlockGenerator
    // For now, create a new instance - in future this could be optimized to share instances
    // Pass null for both RecordExprProcessing and rawExprProcessor as this generator doesn't use expression processing
    this.switchChainBlockGenerator = new SwitchChainBlockGenerator(context, null, null);
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

    // Generate assignment evaluation instructions
    final var assignmentEvaluationInstructions = new ArrayList<>(
        assignExpressionToSymbol.apply(lhsSymbol, assignmentExpression));

    // Delegate to unified SwitchChainBlockGenerator
    return switchChainBlockGenerator.generateGuardedAssignment(
        lhsSymbol,
        assignmentEvaluationInstructions,
        null, // No specific assignment result variable
        basicDetails
    );
  }

}