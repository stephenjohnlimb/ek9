package org.ek9lang.compiler.support;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Given a list of arguments (ISymbols), go through and extract the types out.
 * retaining the ordering of the types.
 */
public class SymbolTypeExtractor implements UnaryOperator<List<ISymbol>> {
  @Override
  public List<ISymbol> apply(List<ISymbol> argumentSymbols) {

    return argumentSymbols.stream()
        .map(ISymbol::getType)
        .filter(Optional::isPresent)
        .flatMap(Optional::stream)
        .toList();
  }
}
