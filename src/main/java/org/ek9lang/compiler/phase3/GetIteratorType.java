package org.ek9lang.compiler.phase3;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Given a 'type' (the IAggregateSymbol passed in), see it would be possible to
 * iterator over it with some other type. If so return that type.
 * This is done by checking for:
 *
 *   <ul>
 *     <li>An iterator() method that returns an Iterator of T</li>
 *     <li>Boolean &lt;- nexNext() AND T &lt;- next()</li>
 *   </ul>
 *
 * <p>Either of the above two mechanism enables iteration over the type (normally a collection).
 * If this is not possible then Optional.empty() is returned.</p>
 */
final class GetIteratorType extends TypedSymbolAccess implements Function<IAggregateSymbol, Optional<ISymbol>> {
  GetIteratorType(final SymbolsAndScopes symbolsAndScopes,
                  final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public Optional<ISymbol> apply(final IAggregateSymbol aggregate) {

    final var resolved = attemptToResolveIterator(aggregate);

    return resolved.isPresent() ? resolved : attemptToResolveHasNextAndNext(aggregate);
  }

  private Optional<ISymbol> attemptToResolveIterator(final IAggregateSymbol aggregate) {

    final var resolved = getMethodReturnType(aggregate, "iterator");
    if (resolved.isPresent() && resolved.get() instanceof IAggregateSymbol expectedIterator) {
      return attemptToResolveHasNextAndNext(expectedIterator);
    }

    return Optional.empty();
  }

  /**
   * Now as this method does exist, check is for 'hasNext()' and 'next()' and if so.
   * check that next() has a return type and that is the type returned.
   */
  private Optional<ISymbol> attemptToResolveHasNextAndNext(final IAggregateSymbol aggregate) {

    //First check if it is an Iterator itself.
    if (aggregate.isParameterisedType()
        && aggregate.getGenericType().isPresent()
        && aggregate.getTypeParameterOrArguments().size() == 1) {

      final var maybeIteratorType = aggregate.getGenericType().get();
      if (maybeIteratorType.isExactSameType(symbolsAndScopes.getEk9Types().ek9Iterator())) {
        //The can only be one and this will have been checked earlier
        return Optional.of(aggregate.getTypeParameterOrArguments().get(0));
      }

    } else {
      //Not a parameterised type with Iterator, but it might have hasNext() and next().
      final var resolvedHasNext = getMethodReturnType(aggregate, "hasNext");
      final var resolvedNext = getMethodReturnType(aggregate, "next");

      if (resolvedHasNext.isPresent()
          && resolvedNext.isPresent()
          && resolvedHasNext.get().isExactSameType(symbolsAndScopes.getEk9Types().ek9Boolean())
          && !resolvedNext.get().isExactSameType(symbolsAndScopes.getEk9Types().ek9Void())) {
        return resolvedNext;
      }

    }

    return Optional.empty();
  }

  private Optional<ISymbol> getMethodReturnType(final IAggregateSymbol aggregate, final String methodName) {

    final var resolved = aggregate.resolve(new MethodSymbolSearch(methodName));

    return resolved.isEmpty() ? Optional.empty() : resolved.get().getType();
  }
}
