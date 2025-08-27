package org.ek9lang.compiler.phase7;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.RecordExprProcessing;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Generates IR instructions for question operator (?) using unified SWITCH_CHAIN_BLOCK.
 * <p>
 * This generator has been migrated to use the unified control flow approach:
 * 1. Delegates to SwitchChainBlockGenerator for actual IR generation  
 * 2. Maintains backward compatibility with existing call sites
 * 3. Uses SWITCH_CHAIN_BLOCK with QUESTION_OPERATOR chain type
 * </p>
 * <p>
 * The Question operator becomes a simple two-case SWITCH_CHAIN_BLOCK:
 * - Case 1: if (operand == null) return Boolean(false)
 * - Default: else return operand._isSet()
 * </p>
 */
public final class QuestionBlockGenerator implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final SwitchChainBlockGenerator switchChainBlockGenerator;

  public QuestionBlockGenerator(final IRContext context,
                                final RecordExprProcessing recordExprProcessing,
                                final Function<ExprProcessingDetails, List<IRInstr>> rawExprProcessor) {
    this.switchChainBlockGenerator = new SwitchChainBlockGenerator(context, recordExprProcessing, rawExprProcessor);
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {
    // Delegate to unified SwitchChainBlockGenerator
    return switchChainBlockGenerator.generateQuestionOperator(details);
  }

  /**
   * Create QUESTION_BLOCK for a variable symbol (used by guarded assignment composition).
   * This method enables composition by allowing other generators to reuse the core
   * question operator logic for variable-based null-safety checking.
   */
  public List<IRInstr> createQuestionBlockForVariable(final ISymbol variableSymbol,
                                                      final String resultName,
                                                      final BasicDetails basicDetails) {
    // Delegate to unified SwitchChainBlockGenerator
    return switchChainBlockGenerator.generateQuestionOperatorForVariable(variableSymbol, resultName, basicDetails);
  }

}