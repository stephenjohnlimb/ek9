package org.ek9lang.compiler.symbols;

import java.util.List;
import java.util.Optional;

/**
 * Interface to abstract FunctionSymbol away as concrete implementation.
 */
public interface IFunctionSymbol extends IScopedSymbol, ICanBeGeneric, IMayReturnSymbol {

  default List<ISymbol> getCallParameters() {
    return List.of();
  }

  default Optional<IFunctionSymbol> getSuperFunction() {
    return Optional.empty();
  }

  default boolean isImplementingInSomeWay(final IFunctionSymbol function) {
    return function == this;
  }

}
