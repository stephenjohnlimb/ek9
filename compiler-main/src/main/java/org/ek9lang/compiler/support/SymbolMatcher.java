package org.ek9lang.compiler.support;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.PercentageMethodSymbolMatch;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Given some search criteria and a List of Symbols, this class will find the best match.
 * The main reason for this is to avoid method name mangling and incorporate coercion,
 * and interface/super type matches.
 */
public class SymbolMatcher implements Serializable {

  //Models the cost of the match
  public static final double ZERO_COST = 0.0;
  public static final double SUPER_COST = 0.05;
  public static final double TRAIT_COST = 0.10;
  public static final double COERCION_COST = 0.5;
  public static final double HIGH_COST = 20.0;
  public static final double INVALID_COST = -1.0;

  //Models the percentage match, the nearer 100.0 the match is the better
  public static final double PERCENT_100 = 100.0;
  public static final double PERCENT_ZERO = 0.0;
  public static final double PERCENT_INVALID = -1.0;

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Match the search criteria against one or more symbol methods.
   * We use a percentage match  algorithm to try and find the best match where there are methods of
   * the same name.
   */
  public void addMatchesToResult(final MethodSymbolSearchResult result,
                                 final SymbolSearch criteria,
                                 final List<MethodSymbol> methodSymbols) {

    AssertValue.checkNotNull("Search cannot be null", criteria);
    AssertValue.checkNotNull("MethodSymbols cannot be null", methodSymbols);

    methodSymbols.forEach(methodSymbol -> {
      final var percentageMatch = getPercentageMethodMatch(criteria, methodSymbol);

      if (percentageMatch >= PERCENT_ZERO) {
        result.add(new PercentageMethodSymbolMatch(methodSymbol, percentageMatch));
      }
    });
  }

  /**
   * Determines how good a fit the search criteria is against the Method Symbol.
   * We match (perfect fit for method name), then return types and parameter types.
   * 100.0 being perfect fit and 0.0 or negative being not fit at all.
   * If between these values we've had to coerce or go via traits or super.s
   *
   * @param criteria     The method criteria we are trying to match
   * @param methodSymbol The method symbol to check against
   * @return The percentage of the match.
   */
  private double getPercentageMethodMatch(final SymbolSearch criteria, final MethodSymbol methodSymbol) {
    double rtn = PERCENT_INVALID;

    if (criteria.getName().equals(methodSymbol.getName())) {

      //Only check if we have a criteria to match - acts more like a veto.
      if (criteria.getOfTypeOrReturn().isPresent()
          && getCostOfMatch(criteria.getOfTypeOrReturn(), methodSymbol.getType()) < ZERO_COST) {
        return rtn;
      }

      //Now need to check on method parameter symbols and match those against the parameters
      //on the method.
      final var paramCost =
          getCostOfParameterMatch(criteria.getTypeParameters(), methodSymbol.getSymbolsForThisScope());
      if (paramCost < ZERO_COST) {
        return rtn;
      }
      rtn = PERCENT_100 - paramCost;
    }
    return rtn;
  }

  /**
   * Calculates the percentage of matching these two lists of parameters. This is useful for
   * calculating matches to overloaded methods and coercions.
   * But if the symbols do not have types then it's an immediate failure to match.
   */
  public double getCostOfParameterMatch(final List<ISymbol> fromSymbols, final List<ISymbol> toSymbols) {

    double rtn = INVALID_COST;

    final var numParams1LookedFor = fromSymbols.size();
    final var numParams2lookedFor = toSymbols.size();

    //So this cannot be a match
    if (numParams1LookedFor != numParams2lookedFor) {
      return rtn;
    }

    //If any symbols do not have a type (due to ek9 developer mistyping types) then cannot be a match.
    if (anyUnTyped(fromSymbols) || anyUnTyped(toSymbols)) {
      return rtn;
    }

    double paramCost = ZERO_COST;
    for (int i = 0; i < numParams1LookedFor; i++) {
      final var fromSymbol = fromSymbols.get(i);
      final var fromType = fromSymbol.getType().orElseThrow();

      ISymbol toSymbol = toSymbols.get(i);
      var toType = toSymbol.getType().orElseThrow();

      final var thisCost = getCostOfSymbolMatch(fromType, toType);
      if (thisCost < ZERO_COST) {
        return rtn; //No match
      }
      paramCost += thisCost;
    }
    rtn = paramCost;

    return rtn;
  }

  private boolean anyUnTyped(final List<ISymbol> symbols) {

    return symbols.stream().map(symbol -> symbol.getType().isEmpty()).findFirst().orElse(false);
  }

  /**
   * Calculates the cost of matching two symbols, which may or may not be present.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public double getCostOfMatch(final Optional<ISymbol> fromSymbol, final Optional<ISymbol> toSymbol) {

    //So neither is set that's Ok
    if (fromSymbol.isEmpty() && toSymbol.isEmpty()) {
      return ZERO_COST;
    }
    if (fromSymbol.isPresent() && toSymbol.isEmpty()) {
      return INVALID_COST;
    }
    if (fromSymbol.isEmpty()) {
      return INVALID_COST;
    }

    //Ok so both set lets see what the cost is

    ISymbol from = fromSymbol.get();
    ISymbol to = toSymbol.get();

    final var costOfMatch = getCostOfSymbolMatch(from, to);
    if (costOfMatch < ZERO_COST) {
      return INVALID_COST; //not a match
    }

    return costOfMatch;
  }

  private double getCostOfSymbolMatch(final ISymbol from, final ISymbol to) {

    return from.getAssignableCostTo(to);
  }
}