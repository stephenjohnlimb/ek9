package org.ek9lang.compiler.support;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.core.Digest;

/**
 * Given some sort of generic type a function or a type with a set of 'type parameters',
 * create a unique internal name for that combination. So this is not a UUID, type operation.
 * But it means when we need to look up a 'org.ek9.lang::List or org.ek9.lang::String' if we have that
 * Generic type already parameterised and recorded - we can just re-use it.
 * This always uses fully qualified type names and then 'digests' that to produce a SHA256 hash.
 * This is highly unlikely to ever collide with another combination of generic types and parameters.
 * <p>
 * But note that the naming takes into account the parameterising types still being conceptual.
 * This
 * </p>
 */
public class NameForParameterisedType implements BiFunction<PossibleGenericSymbol, List<ISymbol>, String> {
  @Override
  public String apply(final PossibleGenericSymbol possibleGenericSymbol, final List<ISymbol> typeArguments) {

    //If the polymorphic type parameters are all conceptual, then this has not actually been
    //parameterised. i.e. Think of a List of type T, being used in a generic implementation of SomeClass of type P.
    //If you then use List of P - it really is still just a 'List of T' only when SomeClass of type P is used
    //with something real like a 'Date', does List of T become a 'List of Date'.

    if (allTypeArgumentsConceptual(typeArguments)) {
      return possibleGenericSymbol.getFullyQualifiedName();
    }

    final var baseGenericType = getBaseGenericType(possibleGenericSymbol);
    final var toDigest = baseGenericType.getFullyQualifiedName() + "_" + typeArguments
        .stream()
        .map(this::getNameForSymbol)
        .collect(Collectors.joining("_"));

    return "_" + baseGenericType.getName() + "_" + Digest.digest(toDigest);
  }

  private String getNameForSymbol(final ISymbol type) {
    if (type.isConceptualTypeParameter()) {
      return "CONCEPTUAL";
    }
    return type.getFullyQualifiedName();
  }

  private boolean allTypeArgumentsConceptual(final List<ISymbol> typeArguments) {
    return typeArguments.stream().allMatch(ISymbol::isConceptualTypeParameter);
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
