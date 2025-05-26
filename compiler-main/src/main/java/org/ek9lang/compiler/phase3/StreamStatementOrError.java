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
 */
final class StreamStatementOrError extends TypedSymbolAccess implements Consumer<EK9Parser.StreamStatementContext> {
  private final StreamAssemblyOrError streamAssemblyOrError;

  StreamStatementOrError(final SymbolsAndScopes symbolsAndScopes,
                         final SymbolFactory symbolFactory,
                         final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.streamAssemblyOrError = new StreamAssemblyOrError(symbolsAndScopes, symbolFactory, errorListener);

  }

  @Override
  public void accept(final EK9Parser.StreamStatementContext ctx) {

    //Lookup in un-typed form, so that the type can be set.
    final var streamStatementSymbol = symbolsAndScopes.getRecordedSymbol(ctx);
    if (streamStatementSymbol != null) {
      setStreamStatementTypeOrError(ctx, streamStatementSymbol);
    }
  }

  private void setStreamStatementTypeOrError(EK9Parser.StreamStatementContext ctx, ISymbol streamStatementSymbol) {

    //Always returns a void because it is a statement - not an expression - that has separate syntax.
    streamStatementSymbol.setType(symbolsAndScopes.getEk9Types().ek9Void());

    //But these must be defined and importantly 'typed'.
    if (getRecordedAndTypedSymbol(ctx.streamSource()) instanceof StreamCallSymbol source
        && getRecordedAndTypedSymbol(ctx.streamStatementTermination()) instanceof StreamCallSymbol termination
        && source.getType().isPresent() && source.getProducesSymbolType() != null
        && termination.getType().isPresent()) {

      final var streamParts = ctx.streamPart();
      streamAssemblyOrError.accept(new StreamAssemblyData(source, streamParts, termination));

    }

  }
}
