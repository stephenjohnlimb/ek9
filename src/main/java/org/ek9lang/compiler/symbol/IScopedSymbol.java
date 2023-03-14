package org.ek9lang.compiler.symbol;

import java.util.List;

/**
 * For symbols that are both a pure symbol but can also define a scope.
 */
public interface IScopedSymbol extends ICanCaptureVariables, ICanBeGeneric, IScope, ISymbol {
  /**
   * Used to keep track of any parameterised types used in a generic type that use some or
   * all of the generic parameters.
   */
  List<ParameterisedTypeSymbol> getParameterisedTypeReferences();

  /**
   * Keep track of parameterised functions used.
   */
  List<ParameterisedFunctionSymbol> getParameterisedFunctionReferences();
}
