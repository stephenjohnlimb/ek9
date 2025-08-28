package org.ek9lang.compiler.phase1;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.PRE_FLOW_OR_CONTROL_REQUIRED;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.core.CompilerException;

/**
 * Because the grammar for pre-flow and control allows both the pre-flow and the control to be omitted
 * it is necessary to check that at least one part is present.
 * <pre>
 *   ifControlBlock
 *   switchStatementExpression
 * </pre>
 */
final class ValidPreFlowAndControlOrError implements Consumer<EK9Parser.PreFlowAndControlContext> {

  private final ErrorListener errorListener;

  ValidPreFlowAndControlOrError(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.PreFlowAndControlContext ctx) {
    if (ctx.preFlowStatement() == null && ctx.control == null) {
      //Then we have an error situation because we need some variable to work with
      //in terms of 'if' and 'switch'
      if (ctx.parent instanceof EK9Parser.IfControlBlockContext ifCtx) {
        errorListener.semanticError(ifCtx.start, "Missing subject of 'if',", PRE_FLOW_OR_CONTROL_REQUIRED);
      } else if (ctx.parent instanceof EK9Parser.SwitchStatementExpressionContext switchCtx) {
        errorListener.semanticError(switchCtx.start, "Missing subject of 'switch',", PRE_FLOW_OR_CONTROL_REQUIRED);
      } else {
        throw new CompilerException("Unexpected parent of preflow and control context");
      }
    }
  }
}
