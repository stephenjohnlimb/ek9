package org.ek9lang.compiler.search;

import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * When searching for methods, this will be a percentage result of how well a method matched.
 */
public class PercentageMethodSymbolMatch {
  private final double percentageMatch;
  private final MethodSymbol methodSymbol;

  public PercentageMethodSymbolMatch(final MethodSymbol methodSymbol, final double percentageMatch) {

    this.percentageMatch = percentageMatch;
    this.methodSymbol = methodSymbol;

  }

  public double getPercentageMatch() {

    return percentageMatch;
  }

  public MethodSymbol getMethodSymbol() {

    return methodSymbol;
  }

  @Override
  public String toString() {
    return methodSymbol.toString() + ": " + percentageMatch;
  }
}
