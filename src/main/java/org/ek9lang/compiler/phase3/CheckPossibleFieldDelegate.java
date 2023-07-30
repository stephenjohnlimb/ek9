package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.support.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.search.MatchResult;
import org.ek9lang.compiler.symbols.search.MatchResults;

/**
 * As it is possible to have a field/property on an aggregate that has a type of 'function',
 * it means that to call that function, the ek9 developer would use field-name(parameters).
 * This is identical in calling a method, if the ek9 developer had methods of the same name as the
 * field - this would be very confusing. Does a method take precedence?, what would haven if some
 * methods became protected or public. So to avoid all that - we just issue an error and force the ek9 developer
 * to use either a different delegate name or different method names.
 * This could lead to a case where two separate components like a trait and a class could not be used together.
 * The answer would be to employ delegation rather than use inheritance.
 */
final class CheckPossibleFieldDelegate extends RuleSupport implements Consumer<ISymbol> {

  private final MostSpecificScope mostSpecificScope;

  /**
   * Create a new field delegate checker.
   */
  CheckPossibleFieldDelegate(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.mostSpecificScope = new MostSpecificScope(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(ISymbol fieldSymbol) {
    if (fieldSymbol != null
        && fieldSymbol.getType().isPresent()
        && fieldSymbol.getType().get() instanceof FunctionSymbol) {
      //Then lets check if its type is some sort of function, this makes it a delegate.
      //Then we must check if the name of the field matches any methods available in this aggregate.
      //If so, that would be very confusing for an ek9 developer, because using the delegate looks
      //very much like a method call. We're ok with other fields having the same name as methods.
      //But while this could be implemented, it would lead to confusion.
      var possibleNameClash = fieldSymbol.getName();
      var scopeToCheckIn = mostSpecificScope.get();
      var matchingSymbols = scopeToCheckIn.getAllSymbolsMatchingName(possibleNameClash);
      //If there are no clashed then this will just contain the one symbol of the field.
      if (matchingSymbols.size() != 1) {
        var msg = "";
        var matches = toMatchResults(fieldSymbol, matchingSymbols);
        errorListener.semanticError(fieldSymbol.getSourceToken(), msg,
            ErrorListener.SemanticClassification.DELEGATE_AND_METHOD_NAMES_CLASH, matches);
      }
    }
  }

  private MatchResults toMatchResults(final ISymbol symbol, final List<ISymbol> otherMatches) {
    MatchResults rtn = new MatchResults(5);
    otherMatches.forEach(matchSymbol -> rtn.add(new MatchResult(symbol.equals(matchSymbol) ? 0 : 1, matchSymbol)));
    return rtn;
  }
}
