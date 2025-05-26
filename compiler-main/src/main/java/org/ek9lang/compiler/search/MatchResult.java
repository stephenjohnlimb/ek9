package org.ek9lang.compiler.search;

import java.util.Comparator;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * There are times when it is necessary to compare a range of symbols to see
 * which is the best match. this is normally when the compiler cannot find a symbol
 * and needs to provide some suggestions for the developer.
 */
public class MatchResult {
  private final int bestWeight;
  private final ISymbol symbol;

  /**
   * Create a new match result of a cost to match for a symbol.
   */
  public MatchResult(final int costOfMatch, final ISymbol symbol) {

    //So we have the highest value for sorting
    //i.e. if a full match then cost is zero and bestWeight is MAX_VALUE
    //but if no match as all costOfMatch is very high and so best weight is lower.
    this.bestWeight = Integer.MAX_VALUE - costOfMatch;
    this.symbol = symbol;
  }

  public static Comparator<MatchResult> getComparator() {

    return Comparator.comparingInt(MatchResult::getBestWeight);
  }

  public ISymbol getSymbol() {

    return symbol;
  }

  public int getBestWeight() {

    return bestWeight;
  }

  @Override
  public String toString() {
    if (symbol.getSourceToken() == null) {
      return symbol.getFriendlyName();
    }
    return symbol.getFriendlyName() + " line: " + symbol.getSourceToken().getLine();
  }
}
