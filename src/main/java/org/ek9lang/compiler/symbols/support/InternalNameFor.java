package org.ek9lang.compiler.symbols.support;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.core.utils.Digest;

/**
 * Given some sort of generic type a function or a type with a set of 'type parameters',
 * create a unique internal name for that combination. So this is not a UUID, type operation.
 * But it means when we need to lookup a 'org.ek9.lang::List or org.ek9.lang::String' if we have that
 * Generic type already parameterised and recorded - we can just re-use it.
 * This always uses fully qualified type names and then 'digests' that to produce a SHA256 hash.
 * This is highly unlikely to ever collide with another combination of generic types and parameters.
 */
public class InternalNameFor implements BiFunction<PossibleGenericSymbol, List<ISymbol>, String> {
  @Override
  public String apply(PossibleGenericSymbol possibleGenericSymbol, List<ISymbol> typeArguments) {
    var toDigest = possibleGenericSymbol.getFullyQualifiedName() + typeArguments
        .stream()
        .map(ISymbol::getFullyQualifiedName)
        .collect(Collectors.joining("_"));

    return "_" + possibleGenericSymbol.getName() + "_" + Digest.digest(toDigest);
  }
}
