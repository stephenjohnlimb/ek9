package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.support.CommonValues.EXTERN;

import java.util.function.Predicate;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Test if a symbol is externally implemented.
 * This normally means it is part of ek9 core or is somehow externally linked to.
 */
public final class ExternallyImplemented implements Predicate<ISymbol> {
  @Override
  public boolean test(final ISymbol symbol) {

    return "TRUE".equals(symbol.getSquirrelledData(EXTERN));
  }
}
