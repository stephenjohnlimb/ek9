package org.ek9lang.compiler.symbols.support;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbols.search.SymbolSearch;
import org.ek9lang.compiler.symbols.search.WeightedMethodSymbolMatch;
import org.ek9lang.core.AssertValue;

/**
 * Given some search criteria and a List of Symbols, this class will find the best match.
 * The main reason for this is to avoid method name mangling and incorporate coercion,
 * and interface/super type matches.
 */
public class SymbolMatcher {

  /**
   * Match the search criteria against one or more symbol methods.
   * We use a weighting algorithm to try and find the best match where there are methods of
   * the same name.
   */
  public void addMatchesToResult(MethodSymbolSearchResult result, SymbolSearch criteria,
                                 List<MethodSymbol> methodSymbols) {
    AssertValue.checkNotNull("Search cannot be null", criteria);
    AssertValue.checkNotNull("MethodSymbols cannot be null", methodSymbols);

    methodSymbols.forEach(methodSymbol -> {
      double weight = getWeightOfMethodMatch(criteria, methodSymbol);

      if (weight >= 0.0) {
        result.add(new WeightedMethodSymbolMatch(methodSymbol, weight));
      }
    });
  }

  /**
   * Determines how good a fit the search criteria is against the Method Symbol.
   * We match (perfect fit for method name), then return types and parameter types.
   * 100.0 being perfect fit and 0.0 or negative being not fit at all.
   * If between these values we've had to coerce
   *
   * @param criteria     The method criteria we are trying to match
   * @param methodSymbol The method symbol to check against
   * @return The weight of the match.
   */
  private double getWeightOfMethodMatch(SymbolSearch criteria, MethodSymbol methodSymbol) {
    double rtn = -1.0;

    if (criteria.getName().equals(methodSymbol.getName())) {

      //Only check if we have a criteria to match - acts more like a veto.
      if (criteria.getOfTypeOrReturn().isPresent()
          && getWeightOfMatch(criteria.getOfTypeOrReturn(), methodSymbol.getType()) < 0.0) {
        return rtn;
      }

      //Now need to check on method parameter symbols and match those against the parameters
      //on the method.
      double paramCost =
          getWeightOfParameterMatch(criteria.getTypeParameters(), methodSymbol.getSymbolsForThisScope());
      if (paramCost < 0.0) {
        return rtn;
      }
      rtn = 100.0 - paramCost;
    }
    return rtn;
  }

  /**
   * Calculates the weight of matching these two lists of parameters. This is useful for
   * calculating matches to overloaded methods and coercions.
   * But if the symbols do not have types then it's an immediate failure to match.
   */
  public double getWeightOfParameterMatch(final List<ISymbol> fromSymbols, final List<ISymbol> toSymbols) {
    double rtn = -1.0;

    int numParams1LookedFor = fromSymbols.size();
    int numParams2lookedFor = toSymbols.size();

    //So this cannot be a match
    if (numParams1LookedFor != numParams2lookedFor) {
      return rtn;
    }

    //If any symbols do not have a type (due to ek9 developer mistyping types) then cannot be a match.
    if (anyUnTyped(fromSymbols) || anyUnTyped(toSymbols)) {
      return rtn;
    }

    double paramCost = 0.0;
    for (int i = 0; i < numParams1LookedFor; i++) {
      ISymbol from = fromSymbols.get(i);
      var fromType = from.getType().orElseThrow();

      ISymbol to = toSymbols.get(i);
      var toType = to.getType().orElseThrow();

      double thisCost = getCostOfSymbolMatch(fromType, toType);
      if (thisCost < 0.0) {
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
   * Calculates te weight of matching two symbols, which may or may not be present.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public double getWeightOfMatch(Optional<ISymbol> fromSymbol, Optional<ISymbol> toSymbol) {
    double rtn = -1.0;

    //So neither is set that's Ok
    if (fromSymbol.isEmpty() && toSymbol.isEmpty()) {
      return 0.0;
    }
    if (fromSymbol.isPresent() && toSymbol.isEmpty()) {
      return rtn;
    }
    if (fromSymbol.isEmpty()) {
      return rtn;
    }

    //Ok so both set lets see what the cost is

    ISymbol from = fromSymbol.get();
    ISymbol to = toSymbol.get();

    double costOfMatch = getCostOfSymbolMatch(from, to);
    if (costOfMatch < 0.0) {
      return rtn; //not a match
    }

    return costOfMatch;
  }

  private double getCostOfSymbolMatch(ISymbol from, ISymbol to) {
    return from.getAssignableWeightTo(to);
  }
}