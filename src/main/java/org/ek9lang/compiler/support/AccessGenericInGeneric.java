package org.ek9lang.compiler.support;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;

/**
 * Uses the symbol and scope management to traverse back up stack to determine if
 * the outer parent is a generic type or not and also if this symbol passed in is a generic type.
 * If so provides the GenericInGenericData structure - else empty.
 */
public class AccessGenericInGeneric implements Function<ISymbol, Optional<GenericInGenericData>> {
  private final SymbolAndScopeManagement symbolAndScopeManagement;

  public AccessGenericInGeneric(final SymbolAndScopeManagement symbolAndScopeManagement) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
  }

  @Override
  public Optional<GenericInGenericData> apply(final ISymbol typeDef) {

    if (typeDef instanceof PossibleGenericSymbol possibleGenericDependent
        && possibleGenericDependent.isGenericInNature()) {

      final var enclosingMainTypeOrFunction = symbolAndScopeManagement.traverseBackUpStack(IScope.ScopeType.NON_BLOCK);

      if (enclosingMainTypeOrFunction.isPresent()
          && enclosingMainTypeOrFunction.get() instanceof PossibleGenericSymbol possibleGenericParent
          && possibleGenericDependent.isGenericInNature()) {
        return Optional.of(new GenericInGenericData(possibleGenericParent, possibleGenericDependent));
      }

    }
    return Optional.empty();
  }
}
