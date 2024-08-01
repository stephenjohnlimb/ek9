package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.StreamCallSymbol;

/**
 * Now does the check/processing of the whole stream pipeline.
 * This focuses on the checking/population of consumes/produces of types in each of
 * the stages of the pipeline from the sources, pipe-line-parts* and termination.
 * This is quite tricky because some things types are fixed and others more flexible.
 */
final class StreamExpressionOrError extends TypedSymbolAccess implements Consumer<EK9Parser.StreamExpressionContext> {
  private final StreamAssemblyOrError streamAssemblyOrError;

  StreamExpressionOrError(final SymbolsAndScopes symbolsAndScopes,
                          final SymbolFactory symbolFactory,
                          final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.streamAssemblyOrError = new StreamAssemblyOrError(symbolsAndScopes, symbolFactory, errorListener);

  }

  @Override
  public void accept(final EK9Parser.StreamExpressionContext ctx) {

    //Just get the symbol, but do not expect it to be typed (yet, see below).
    final var streamExpressionSymbol = symbolsAndScopes.getRecordedSymbol(ctx);
    if (streamExpressionSymbol != null) {
      setStreamExpressionTypeOrError(ctx, streamExpressionSymbol);
    }
  }

  private void setStreamExpressionTypeOrError(final EK9Parser.StreamExpressionContext ctx,
                                              final ISymbol streamExpressionSymbol) {

    //But these must be defined and importantly 'typed'.
    if (getRecordedAndTypedSymbol(ctx.streamSource()) instanceof StreamCallSymbol source
        && getRecordedAndTypedSymbol(ctx.streamExpressionTermination()) instanceof StreamCallSymbol termination
        && source.getType().isPresent() && source.getProducesSymbolType() != null
        && termination.getType().isPresent()) {

      final var streamParts = ctx.streamPart();
      //Now ensure that the whole stream assembly is valid
      streamAssemblyOrError.accept(new StreamAssemblyData(source, streamParts, termination));
      //As this is an expression - it will return whatever the final type is for the 'collect'.
      streamExpressionSymbol.setType(termination.getType());

    }

  }
}
