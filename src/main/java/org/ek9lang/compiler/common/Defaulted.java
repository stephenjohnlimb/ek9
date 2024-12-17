package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.support.CommonValues.DEFAULTED;

import java.util.function.Predicate;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Test if a symbol is externally implemented.
 * This normally means it is part of ek9 core or is somehow externally linked to.
 */
public final class Defaulted implements Predicate<ISymbol> {
  @Override
  public boolean test(final ISymbol symbol) {

    return "TRUE".equals(symbol.getSquirrelledData(DEFAULTED));
  }
}
