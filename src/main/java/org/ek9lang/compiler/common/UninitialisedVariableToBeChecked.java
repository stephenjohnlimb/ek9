package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.support.SymbolFactory.EXTERN;
import static org.ek9lang.compiler.support.SymbolFactory.UNINITIALISED_AT_DECLARATION;

import java.util.function.Predicate;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;

/**
 * Checks if a variable should be processed as a possible uninitialised variable.
 * There are some cases where the uninitialised checks are not undertaken.
 */
public final class UninitialisedVariableToBeChecked implements Predicate<ISymbol> {
  @Override
  public boolean test(ISymbol symbol) {
    return ("TRUE".equals(symbol.getSquirrelledData(UNINITIALISED_AT_DECLARATION))
        && !"TRUE".equals(symbol.getSquirrelledData(EXTERN))
        && symbol instanceof VariableSymbol && !symbol.isPropertyField());
  }
}
