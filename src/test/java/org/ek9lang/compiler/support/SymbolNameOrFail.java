package org.ek9lang.compiler.support;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Just return the name of the Symbol in the Option or exception if nothing present.
 */
public class SymbolNameOrFail implements Function<Optional<ISymbol>, String> {
  @Override
  public String apply(Optional<ISymbol> symbol) {
    return symbol.map(ISymbol::getName).orElseThrow();
  }
}
