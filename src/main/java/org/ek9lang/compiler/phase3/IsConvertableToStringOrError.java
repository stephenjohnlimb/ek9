package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TYPE_MUST_BE_CONVERTABLE_TO_STRING;

import java.util.function.BiPredicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.TypeCoercions;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.CompilerException;

/**
 * Checks if a type has the $ 'toString' method or a 'promotion' #? to a String method.
 * If not emits and error. Or it must be the String type itself.
 */
class IsConvertableToStringOrError extends RuleSupport implements BiPredicate<IToken, ISymbol> {

  private final TypeCoercions typeCoercions = new TypeCoercions();

  /**
   * Constructor to provided typed access.
   */
  IsConvertableToStringOrError(final SymbolsAndScopes symbolsAndScopes,
                               final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
  }

  @Override
  public boolean test(final IToken errorReportingLocation, final ISymbol typeSymbol) {
    final var stringType = symbolsAndScopes.getEk9Types().ek9String();

    if (typeSymbol instanceof IAggregateSymbol asAggregate) {

      if (stringType.isExactSameType(typeSymbol)) {
        return true;
      }

      if (!asAggregate.resolveMatchingMethods(new MethodSymbolSearch("$"), new MethodSymbolSearchResult())
          .isEmpty()) {
        return true;
      }

      if (!typeCoercions.isCoercible(typeSymbol, stringType)) {
        emitIsNotConvertableToString(errorReportingLocation, typeSymbol);
      }

    } else {
      throw new CompilerException("Expecting an aggregate as the type to be checked");
    }

    return false;
  }

  private void emitIsNotConvertableToString(final IToken errorReportingLocation, final ISymbol typeSymbol) {

    final var msg = "'" + typeSymbol.getName() + "'";
    errorListener.semanticError(errorReportingLocation, msg, TYPE_MUST_BE_CONVERTABLE_TO_STRING);

  }
}
