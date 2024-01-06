package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;

/**
 * Now does the check/processing of the whole stream pipeline.
 * This focuses on the checking/population of consumes/produces of types in each of
 * the stages of the pipeline from the sources, pipe-line-parts* and termination.
 * This is quite tricky because some things types are fixed and others more flexible.
 */
final class ProcessStreamExpression extends TypedSymbolAccess implements Consumer<EK9Parser.StreamExpressionContext> {
  ProcessStreamExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                          final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.StreamExpressionContext ctx) {
    //TODO
  }
}
