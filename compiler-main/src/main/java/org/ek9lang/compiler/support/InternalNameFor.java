package org.ek9lang.compiler.support;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;

/**
 * Given some sort of generic type a function or a type with a set of 'type parameters',
 * create a unique internal name for that combination. So this is not a UUID, type operation.
 * But it means when we need to look up a 'org.ek9.lang::List or org.ek9.lang::String' if we have that
 * Generic type already parameterised and recorded - we can just re-use it.
 * This always uses fully qualified type names and then 'digests' that to produce a SHA256 hash.
 * This is highly unlikely to ever collide with another combination of generic types and parameters.
 */
public class InternalNameFor implements BiFunction<PossibleGenericSymbol, List<ISymbol>, String> {
  private final SameGenericConceptualParameters sameGenericConceptualParameters = new SameGenericConceptualParameters();

  private final DecoratedName decoratedName = new DecoratedName();

  @Override
  public String apply(final PossibleGenericSymbol possibleGenericSymbol, final List<ISymbol> typeArguments) {

    if (sameGenericConceptualParameters.test(possibleGenericSymbol, typeArguments)) {
      return possibleGenericSymbol.getFullyQualifiedName();
    }

    final var baseGenericType = getBaseGenericType(possibleGenericSymbol);

    final var details = new InternalNameDetails(possibleGenericSymbol.getName(),
        baseGenericType.getFullyQualifiedName(),
        typeArguments.stream().map(ISymbol::getFullyQualifiedName).toList());

    return decoratedName.apply(details);

  }

  /**
   * Given the fact that it is possible to partially parameterize a polymorphic type with
   * a mix of concrete and conceptual type arguments, we need to get to the base, which means traversing the
   * hierarchy.
   */
  private PossibleGenericSymbol getBaseGenericType(final PossibleGenericSymbol possibleGenericSymbol) {
    var possibleGenericSuper = possibleGenericSymbol.getGenericType();
    return possibleGenericSuper.map(this::getBaseGenericType).orElse(possibleGenericSymbol);
  }
}
