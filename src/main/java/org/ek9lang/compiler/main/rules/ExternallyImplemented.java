package org.ek9lang.compiler.main.rules;

import static org.ek9lang.compiler.symbols.support.SymbolFactory.EXTERN;

import java.util.function.Predicate;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Test if a symbol is externally implemented.
 * This normally means it is part of ek9 core or is somehow externally linked to.
 */
public class ExternallyImplemented implements Predicate<ISymbol> {
  @Override
  public boolean test(ISymbol symbol) {
    return "TRUE".equals(symbol.getSquirrelledData(EXTERN));
  }
}
