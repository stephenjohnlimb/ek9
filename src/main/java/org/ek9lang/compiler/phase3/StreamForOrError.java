package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.StreamCallSymbol;

/**
 * Deals with working out what the type being "for'd" is.
 * Unlike 'cat' this is basically the same type as the 'loop' iterator.
 * So most of this work has been done in the 'for range' part when produces a type.
 * But obviously if the 'for range' is broken then the type will not be known.
 */
final class StreamForOrError extends TypedSymbolAccess implements Consumer<EK9Parser.StreamForContext> {

  StreamForOrError(final SymbolsAndScopes symbolsAndScopes,
                   final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.StreamForContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof StreamCallSymbol streamFor) {
      final var forRange = getRecordedAndTypedSymbol(ctx.forRange());
      //If null or untyped errors will have been emitted
      if (forRange != null) {
        forRange.getType().ifPresent(type -> {
          streamFor.setProducesSymbolType(type);
          streamFor.setType(type);
        });
      }

    }
  }

}
