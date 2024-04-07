package org.ek9lang.compiler.support;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Given a list of symbols (normally variables), this code will get the type from each of those symbols.
 * See SymbolTypeExtractor for this.
 * It will then determine if the first type is an aggregate or a function.
 * If it is a function - then it will check that all the other types in the list are also functions and
 * if they are the 'same' function, typically this will be false (for functions), it will then check the
 * super (if present) and check if the other functions also have the same super. This will continue going up
 * the supers until they are the same or there are no more supers.
 * For aggregates, the same process if followed for supers, except if there is no common super the code looks
 * for a common 'trait'.
 * If any of the variableSymbols have not been typed then this function will return an empty Optional.
 * If there are no common supers then this function will return an empty Optional.
 * This function will issue semantic errors.
 */
public class CommonTypeSuperOrTrait implements BiFunction<IToken, List<ISymbol>, Optional<ISymbol>> {
  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final CommonTypeOrError commonTypeOrError;

  public CommonTypeSuperOrTrait(final ErrorListener errorListener) {

    this.commonTypeOrError = new CommonTypeOrError(errorListener);

  }

  @Override
  public Optional<ISymbol> apply(final IToken lineToken, final List<ISymbol> argumentSymbols) {

    if (argumentSymbols.isEmpty()) {
      return Optional.empty();
    }

    final var argumentTypes = symbolTypeExtractor.apply(argumentSymbols);

    if (argumentTypes.size() != argumentSymbols.size()) {
      //No error issued as other code should have detected this.
      return Optional.empty();
    }

    final var details = new CommonTypeDeterminationDetails(lineToken, argumentSymbols, argumentTypes);

    return commonTypeOrError.apply(details);
  }
}
