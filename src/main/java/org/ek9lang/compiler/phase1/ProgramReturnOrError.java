package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.support.AggregateFactory;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * A program can only return an Integer (exit code).
 */
final class ProgramReturnOrError implements BiConsumer<IToken, MethodSymbol> {
  private final ErrorListener errorListener;

  ProgramReturnOrError(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final IToken token, final MethodSymbol methodSymbol) {

    if (methodSymbol.isReturningSymbolPresent()) {
      final var returningSymbol = methodSymbol.getReturningSymbol();
      final var returningSymbolsType = returningSymbol.getType();

      if (returningSymbolsType.isEmpty() || !AggregateFactory.EK9_INTEGER.equals(
          returningSymbolsType.get().getFullyQualifiedName())) {
        errorListener.semanticError(returningSymbol.getSourceToken(), "",
            ErrorListener.SemanticClassification.PROGRAM_CAN_ONLY_RETURN_INTEGER);
      }
    }

  }
}
