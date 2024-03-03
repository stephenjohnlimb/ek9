package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.StreamCallSymbol;

/**
 * Now does the check/processing of the whole stream pipeline.
 * This focuses on the checking/population of consumes/produces of types in each of
 * the stages of the pipeline from the sources, pipe-line-parts* and termination.
 * This is quite tricky because some things types are fixed and others more flexible.
 */
final class ProcessStreamExpression extends TypedSymbolAccess implements Consumer<EK9Parser.StreamExpressionContext> {
  private final ProcessStreamAssembly processStreamAssembly;

  ProcessStreamExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                          final SymbolFactory symbolFactory,
                          final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.processStreamAssembly = new ProcessStreamAssembly(symbolAndScopeManagement, symbolFactory, errorListener);
  }

  @Override
  public void accept(final EK9Parser.StreamExpressionContext ctx) {
    var streamExpressionSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx);

    //We expect these to be defined and typed.
    var source = (StreamCallSymbol) getRecordedAndTypedSymbol(ctx.streamSource());
    var termination = (StreamCallSymbol) getRecordedAndTypedSymbol(ctx.streamExpressionTermination());
    var streamParts = ctx.streamPart();

    //Otherwise there will have been errors emitted.
    if (source != null && source.getType().isPresent()
        && source.getProducesSymbolType() != null
        && termination != null && termination.getType().isPresent()) {
      processStreamAssembly.accept(new StreamAssembly(source, streamParts, termination));
      //As this is an expression - it will return whatever the final type is for the 'collect'.
      streamExpressionSymbol.setType(termination.getType());
    }
  }
}
