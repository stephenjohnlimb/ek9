package org.ek9lang.compiler.symbols.support;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Just return the name of the Type for the Symbol in the Option or exception if nothing present.
 */
public class SymbolTypeNameOrFail implements Function<Optional<ISymbol>, String> {

  private final SymbolNameOrFail symbolNameExtractor = new SymbolNameOrFail();

  @Override
  public String apply(Optional<ISymbol> symbol) {
    return symbol.map(s -> symbolNameExtractor.apply(s.getType())).orElseThrow();
  }
}
