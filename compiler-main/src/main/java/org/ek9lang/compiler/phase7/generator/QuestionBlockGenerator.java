package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;

/**
 * Generates IR instructions for question operator (?) using unified SWITCH_CHAIN_BLOCK.
 * <p>
 * This generator has been migrated to use the unified control flow approach:<br>
 * 1. Delegates to ControlFlowChainGenerator for actual IR generation<br>
 * 2. Maintains backward compatibility with existing call sites<br>
 * 3. Uses CONTROL_FLOW_CHAIN with QUESTION_OPERATOR chain type<br>
 * </p>
 * <p>
 * The Question operator becomes a simple two-case CONTROL_FLOW_CHAIN:<br>
 * - Case 1: if (operand == null) return Boolean(false)<br>
 * - Default: else return operand._isSet()<br>
 * </p>
 */
public final class QuestionBlockGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final ControlFlowChainGenerator controlFlowChainGenerator;

  public QuestionBlockGenerator(final IRGenerationContext stackContext,
                                final Function<ExprProcessingDetails, List<IRInstr>> rawExprProcessor) {
    super(stackContext);
    this.controlFlowChainGenerator = new ControlFlowChainGenerator(stackContext, rawExprProcessor);
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {
    // Delegate to unified ControlFlowChainGenerator
    return controlFlowChainGenerator.generateQuestionOperator(details);
  }

}