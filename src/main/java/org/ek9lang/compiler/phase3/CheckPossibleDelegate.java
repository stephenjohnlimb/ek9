package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MatchResult;
import org.ek9lang.compiler.search.MatchResults;
import org.ek9lang.compiler.support.MostSpecificScope;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * As it is possible to have a variable that is a delegate 'function',
 * it means that to call that function, the ek9 developer would use variable-name(parameters).
 * This is identical in calling a method, if the ek9 developer had methods of the same name as the
 * variable - this would be very confusing. Does a method take precedence?, what would haven if some
 * methods became protected or public. So to avoid all that - we just issue an error and force the ek9 developer
 * to use either a different delegate name or different method names.
 * This could lead to a case where two separate components like a trait and a class could not be used together.
 * The answer would be to employ delegation rather than use inheritance.
 */
final class CheckPossibleDelegate extends TypedSymbolAccess implements Consumer<ISymbol> {
  private final MostSpecificScope mostSpecificScope;

  /**
   * Create a new delegate checker.
   */
  CheckPossibleDelegate(final SymbolAndScopeManagement symbolAndScopeManagement,
                        final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
    this.mostSpecificScope = new MostSpecificScope(symbolAndScopeManagement);

  }

  @Override
  public void accept(final ISymbol symbol) {

    if (symbol != null
        && symbol.getType().isPresent()
        && symbol.getType().get() instanceof FunctionSymbol) {
      //Then lets check if its type is some sort of function, that makes it a delegate.
      //Then we must check if the name of the field matches any methods available in this aggregate.
      final var possibleNameClash = symbol.getName();
      final var scopeToCheckIn = mostSpecificScope.get();
      final var matchingSymbols = scopeToCheckIn.getAllSymbolsMatchingName(possibleNameClash);

      if (hasClashingSymbolNames(matchingSymbols, symbol)) {
        final var matches = toMatchResults(symbol, matchingSymbols);
        errorListener.semanticError(symbol.getSourceToken(), "",
            ErrorListener.SemanticClassification.DELEGATE_AND_METHOD_NAMES_CLASH, matches);
      }
    }

  }

  private boolean hasClashingSymbolNames(final List<ISymbol> symbols, final ISymbol toExclude) {

    //We do allow an incoming parameter to be the same name as a method. At the moment!
    if (!toExclude.isIncomingParameter()) {
      return symbols.stream().anyMatch(symbol -> !symbol.equals(toExclude));
    }

    return symbols.size() > 1;
  }

  private MatchResults toMatchResults(final ISymbol symbol, final List<ISymbol> otherMatches) {

    MatchResults rtn = new MatchResults(5);
    otherMatches.forEach(matchSymbol -> rtn.add(new MatchResult(symbol.equals(matchSymbol) ? 0 : 1, matchSymbol)));

    return rtn;
  }
}
