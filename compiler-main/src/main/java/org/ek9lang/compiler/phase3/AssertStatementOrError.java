package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.ONLY_COMPATIBLE_WITH_BOOLEAN;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Checks that the assert statement actually has a boolean result from the expression.
 * Because basically it is an if 'boolean' all ok, else throw some sort of exception.
 */
final class AssertStatementOrError extends TypedSymbolAccess implements Consumer<EK9Parser.AssertStatementContext> {

  AssertStatementOrError(final SymbolsAndScopes symbolsAndScopes,
                         final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.AssertStatementContext ctx) {

    final var errorLocation = new Ek9Token(ctx.expression().start);

    //Now we need to cater for errors in the developers code so lets check we can access a recorded symbol.
    final var expressionSymbol = symbolsAndScopes.getRecordedSymbol(ctx.expression());
    if (expressionSymbol != null) {
      //We must also accept that at this point in time there could be other errors that cause the type not to be known.
      expressionSymbol.getType().ifPresent(resultingType -> {
        final var validType = symbolsAndScopes.getEk9Types().ek9Boolean().isExactSameType(resultingType);

        if (!validType) {
          errorListener.semanticError(errorLocation, "Expression resulting in type '"
                  + resultingType.getFriendlyName() + "' is not compatible with assert statement:",
              ONLY_COMPATIBLE_WITH_BOOLEAN);
        }
      });

    }
  }
}
