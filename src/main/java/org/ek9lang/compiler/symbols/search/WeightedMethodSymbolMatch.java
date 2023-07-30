package org.ek9lang.compiler.symbols.search;

import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * When searching for methods, this will be a weighted result of how well a method matched.
 */
public class WeightedMethodSymbolMatch {
  private final double weight;
  private final MethodSymbol methodSymbol;

  public WeightedMethodSymbolMatch(MethodSymbol methodSymbol, double matchWeight) {
    this.weight = matchWeight;
    this.methodSymbol = methodSymbol;
  }

  public double getWeight() {
    return weight;
  }

  public MethodSymbol getMethodSymbol() {
    return methodSymbol;
  }

  @Override
  public String toString() {
    return methodSymbol.toString() + ": " + weight;
  }
}
