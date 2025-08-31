package org.ek9lang.compiler.phase5;

import java.util.function.Predicate;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Tests specific types of variable of specific types (generic Result/Option) in a manner
 * that enables the compiler to check if access to certain methods is safe.
 */
final class SafeGenericAccessPredicate implements Predicate<ISymbol> {

  private final SymbolsAndScopes symbolsAndScopes;
  private final HasTypeOfGeneric resultTypeCheck;
  private final HasTypeOfGeneric optionalTypeCheck;
  private final HasTypeOfGeneric iteratorTypeCheck;

  /**
   * Constructor to provided typed access.
   */
  SafeGenericAccessPredicate(final SymbolsAndScopes symbolsAndScopes) {
    this.symbolsAndScopes = symbolsAndScopes;
    this.resultTypeCheck = new HasTypeOfGeneric(symbolsAndScopes.getEk9Types().ek9Result());
    this.optionalTypeCheck = new HasTypeOfGeneric(symbolsAndScopes.getEk9Types().ek9Optional());
    this.iteratorTypeCheck = new HasTypeOfGeneric(symbolsAndScopes.getEk9Types().ek9Iterator());
  }

  @Override
  public boolean test(final ISymbol symbol) {

    if (resultTypeCheck.test(symbol)) {
      return symbolsAndScopes.isOkResultAccessSafe(symbol);
    } else if (optionalTypeCheck.test(symbol)) {
      return symbolsAndScopes.isGetOptionalAccessSafe(symbol);
    } else if (iteratorTypeCheck.test(symbol)) {
      return symbolsAndScopes.isNextIteratorAccessSafe(symbol);
    }
    return false;
  }

}
