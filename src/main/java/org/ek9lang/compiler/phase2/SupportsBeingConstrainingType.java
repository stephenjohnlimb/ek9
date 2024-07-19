package org.ek9lang.compiler.phase2;

import java.util.function.Predicate;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Only types of a specific genus can be used as constraints on the generic type 'T'.
 */
final class SupportsBeingConstrainingType implements Predicate<ISymbol> {

  @Override
  public boolean test(final ISymbol symbol) {

    return symbol != null && switch (symbol.getGenus()) {
      case COMPONENT,
          CLASS,
          CLASS_TRAIT,
          CLASS_CONSTRAINED,
          CLASS_ENUMERATION,
          RECORD,
          TYPE -> true;
      default -> false;
    };

  }
}
