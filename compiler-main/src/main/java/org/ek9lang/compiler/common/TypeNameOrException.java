package org.ek9lang.compiler.common;

import java.util.function.Function;
import org.ek9lang.compiler.symbols.IMayReturnSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * Extracts the fully qualified type name from the symbol.
 * Or throws an Exception if the symbol is un-typed. Missing types should have been detected in earlier stages.
 * So be carful which phase you use this with.
 */
public final class TypeNameOrException implements Function<ISymbol, String> {

  @Override
  public String apply(final ISymbol symbol) {

    if (symbol instanceof IMayReturnSymbol mayReturnSymbol && mayReturnSymbol.isReturningSymbolPresent()) {
      final var returningSymbol = mayReturnSymbol.getReturningSymbol();
      return getFullyQualifiedTypeName(returningSymbol);
    }

    // Check if symbol itself has a type (for functions without explicit return symbol)
    return getFullyQualifiedTypeName(symbol);
  }

  private String getFullyQualifiedTypeName(final ISymbol symbol) {
    return symbol.getType()
        .map(ISymbol::getFullyQualifiedName)
        .orElseThrow(() -> new CompilerException("Type should be known"));
  }
}
