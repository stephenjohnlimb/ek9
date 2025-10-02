package org.ek9lang.compiler.phase7.support;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_ANY;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_LANG;
import static org.ek9lang.compiler.symbols.SymbolGenus.CLASS_TRAIT;

import java.util.function.Predicate;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks if the ISymbol as a type passed in is deemed to be an implicit super.
 * This typically means it's an inferred function or class. Generally something where
 * we should not be calling any for of super method on it.
 */
public class NotImplicitSuper implements Predicate<ISymbol> {


  @Override
  public boolean test(final ISymbol symbol) {
    if (CLASS_TRAIT == symbol.getGenus()) {
      return true;
    }
    final var fqn = symbol.getFullyQualifiedName();
    return !fqn.startsWith(EK9_LANG + "::_") && !fqn.equals(EK9_ANY);

  }
}
