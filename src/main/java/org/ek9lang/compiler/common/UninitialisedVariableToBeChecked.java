package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.support.CommonValues.UNINITIALISED_AT_DECLARATION;

import java.util.function.Predicate;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;

/**
 * Checks if a variable should be processed as a possible uninitialised variable.
 * There are some cases where the uninitialised checks are not undertaken.
 */
public final class UninitialisedVariableToBeChecked implements Predicate<ISymbol> {
  private final ExternallyImplemented externallyImplemented = new ExternallyImplemented();

  @Override
  public boolean test(final ISymbol symbol) {

    return ("TRUE".equals(symbol.getSquirrelledData(UNINITIALISED_AT_DECLARATION))
        && !externallyImplemented.test(symbol)
        && symbol instanceof VariableSymbol && !symbol.isPropertyField());
  }
}
