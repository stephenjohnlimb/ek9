package org.ek9lang.compiler.support;

import java.io.File;
import java.util.function.Function;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Extracts a string for the form "on line 6 in 'filename.ek9'".
 * Typically used for error reporting.
 */
public class LocationExtractor implements Function<ISymbol, String> {
  @Override
  public String apply(ISymbol symbol) {
    var token = symbol.isParameterisedType() ? symbol.getInitialisedBy() : symbol.getSourceToken();

    var fileName = new File(token.getSourceName()).getName();
    return String.format("on line %s in '%s'", token.getLine(), fileName);
  }
}
