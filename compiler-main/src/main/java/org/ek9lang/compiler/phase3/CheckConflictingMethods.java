package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.METHODS_CONFLICT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Checks that methods from a super and one or more traits do not conflict.
 * This is due to the fact that both a super and also one or more traits can actually have
 * implementations of the same method signature.
 * The solution is to raise an error and get the ek9 developer to create a new method in the
 * aggregate that is using the super/traits and then pick the implementation they want or define a
 * totally new one.
 * Returns true if there are no conflicting methods.
 * Now this may mean because all constructs inherit from 'Any', then there a multiple routes to Any.
 * Also, if we have a defined implementation anywhere, then we want to evict the Any method because that's just
 * a default no operation.
 */
final class CheckConflictingMethods extends TypedSymbolAccess implements Predicate<AggregateSymbol> {
  CheckConflictingMethods(final SymbolsAndScopes symbolsAndScopes,
                          final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public boolean test(final AggregateSymbol symbol) {
    var clashFree = true;
    //Used to quickly lookup any potentially clashing methods.
    final Map<String, ArrayList<MethodSymbol>> lookup = new HashMap<>();

    //Get the methods that are actually in effect.
    final var allMethods = symbol.getAllEffectiveMethods();

    for (var method : allMethods) {
      //We don't check for Constructor clashes, to avoid issues with 'Any()' constructor on traits.
      if (lookup.containsKey(method.getName()) && !method.isConstructor()) {

        final var possibleClash = getAnyClashingMethod(lookup, method);
        if (possibleClash.isPresent()) {
          //We have a duplicate
          final var msg = String.format("method '%s' on '%s' and '%s' :",
              method.getFriendlyName(), method.getParentScope().getFriendlyScopeName(),
              possibleClash.get().getParentScope().getFriendlyScopeName());

          errorListener.semanticError(symbol.getSourceToken(), msg, METHODS_CONFLICT);
          clashFree = false;
        }
      } else {
        final var methodList = new ArrayList<MethodSymbol>();
        methodList.add(method);
        lookup.put(method.getName(), methodList);
      }
    }

    //So if there are any duplicate methods (i.e. with same signature), then that's an error.
    //It means that the EK9 developer must add an overriding method in to decide on the functionality
    return clashFree;
  }

  private Optional<MethodSymbol> getAnyClashingMethod(final Map<String, ArrayList<MethodSymbol>> lookup,
                                                      final MethodSymbol method) {

    //Gte name of method, look for list of matching methods in the list of methods of the same name/signature.
    final var methodList = lookup.get(method.getName());
    final var clashingMatches = matchingMethods(methodList, method);

    //Ok nothing clashes, so add that one in and we're all good.
    if (clashingMatches.isEmpty()) {
      methodList.add(method);
      return Optional.empty();
    }

    //Otherwise we have a duplicate, but here we want to see if the one in the list is on 'Any'. if so we will
    //evict it and use this new method (which could also be on Any, but that's not important, because Any is fine if
    //that's all there is - but because of traits and classes, it is possible to find Any by many routes.
    //This is not ambiguous, it's clear - just call 'Any' / operator like ? for example
    if (symbolsAndScopes.getEk9Types().ek9Any()
        .isExactSameType((ISymbol) clashingMatches.getFirst().getParentScope())) {
      methodList.removeFirst();
      methodList.add(method);
      return Optional.empty();
    }

    return Optional.of(clashingMatches.getFirst());
  }


  private List<MethodSymbol> matchingMethods(final List<MethodSymbol> defined, final MethodSymbol checkMethod) {

    return defined.stream()
        .filter(method -> methodsMatch(method, checkMethod))
        .toList();

  }

  private boolean methodsMatch(final MethodSymbol m1, final MethodSymbol m2) {

    return m1.getName().equals(m2.getName()) && m1.isExactSignatureMatchTo(m2);

  }

}
