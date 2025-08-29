package org.ek9lang.compiler.phase7;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRContext;

/**
 * Generates IR instructions for question operator (?) using unified SWITCH_CHAIN_BLOCK.
 * <p>
 * This generator has been migrated to use the unified control flow approach:
 * 1. Delegates to ControlFlowChainGenerator for actual IR generation
 * 2. Maintains backward compatibility with existing call sites
 * 3. Uses CONTROL_FLOW_CHAIN with QUESTION_OPERATOR chain type
 * </p>
 * <p>
 * The Question operator becomes a simple two-case CONTROL_FLOW_CHAIN:
 * - Case 1: if (operand == null) return Boolean(false)
 * - Default: else return operand._isSet()
 * </p>
 */
public final class QuestionBlockGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final ControlFlowChainGenerator controlFlowChainGenerator;

  public QuestionBlockGenerator(final IRContext context,
                                final Function<ExprProcessingDetails, List<IRInstr>> rawExprProcessor) {
    super(context);
    this.controlFlowChainGenerator = new ControlFlowChainGenerator(context, rawExprProcessor);
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {
    // Delegate to unified ControlFlowChainGenerator
    return controlFlowChainGenerator.generateQuestionOperator(details);
  }

}