package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.METHODS_CONFLICT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Checks that methods from a super and one or more traits do not conflict.
 * This is due to the fact that both a super and also one or more traits can actually have
 * implementations of the same method signature.
 * The solution is to raise an error and get the ek9 developer to create a new method in the
 * aggregate that is using the super/traits and then pick the implementation they want or define a
 * totally new one.
 * Returns true if there are no conflicting methods.
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
    final var lookup = new HashMap<String, ArrayList<MethodSymbol>>();

    //Get the methods that are actually in effect.
    final var allMethods = symbol.getAllEffectiveMethods();

    for (var method : allMethods) {
      if (lookup.containsKey(method.getName())) {
        final var methodList = lookup.get(method.getName());
        final var clashingMatches = matchingMethods(methodList, method);
        if (clashingMatches.isEmpty()) {
          methodList.add(method);
        } else {
          //We have a duplicate
          final var msg = "method '" + method.getFriendlyName()
              + "' on '"
              + method.getParentScope().getFriendlyScopeName()
              + "' and '"
              + clashingMatches.get(0).getParentScope().getFriendlyScopeName()
              + "':";
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


  private List<MethodSymbol> matchingMethods(final List<MethodSymbol> defined, final MethodSymbol checkMethod) {

    return defined.stream()
        .filter(method -> methodsMatch(method, checkMethod))
        .toList();

  }

  private boolean methodsMatch(final MethodSymbol m1, final MethodSymbol m2) {

    return m1.getName().equals(m2.getName()) && m1.isExactSignatureMatchTo(m2);

  }

}
