package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.symbols.ISymbol.SymbolGenus.VALUE;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.StreamCallSymbol;

/**
 * Process and ensure that the stream termination can function correctly.
 * But note this is only a basic check, only at the end of the whole Stream pipeline 'exit'
 * can the whole 'typed' flow be checked.
 */
final class StreamExpressionTerminationOrError extends TypedSymbolAccess implements
    Consumer<EK9Parser.StreamExpressionTerminationContext> {

  StreamExpressionTerminationOrError(final SymbolsAndScopes symbolsAndScopes,
                                     final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.StreamExpressionTerminationContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof StreamCallSymbol terminationSymbol) {
      final var collectAsType = getRecordedAndTypedSymbol(ctx.typeDef());

      //Otherwise an error will have been emitted.
      if (collectAsType != null && collectAsType.getType().isPresent()) {
        terminationSymbol.setType(collectAsType);
        terminationSymbol.setGenus(VALUE);
      }
    }

  }
}
