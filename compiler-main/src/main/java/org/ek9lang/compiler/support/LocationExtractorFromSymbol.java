package org.ek9lang.compiler.support;

import java.util.function.Function;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Extracts a string for the form "on line 6 in 'filename.ek9'".
 * Typically used for error reporting.
 */
public class LocationExtractorFromSymbol implements Function<ISymbol, String> {
  private final LocationExtractorFromToken locationExtractorFromToken = new LocationExtractorFromToken();

  @Override
  public String apply(final ISymbol symbol) {

    final var token = symbol.isParameterisedType() ? symbol.getInitialisedBy() : symbol.getSourceToken();

    return locationExtractorFromToken.apply(token);

  }
}
