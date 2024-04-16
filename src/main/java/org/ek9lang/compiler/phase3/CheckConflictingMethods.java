package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.METHODS_CONFLICT;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Checks that methods from a super and one or more traits do not conflict.
 * This is due to the fact that both a super and also one or more traits can actually have
 * implementations of the same method signature.
 * The solution is to raise an error and get the ek9 developer to create a new method in the
 * aggregate that is using the super/traits and then pick the implementation they want or define a
 * totally new one.
 * Returns true if there are no conflicting methods.
 * TODO not quite sure this is working correctly. See JustTraits.ek9 - ExamplesConstructsTraitsTest.
 */
final class CheckConflictingMethods extends TypedSymbolAccess implements Predicate<AggregateSymbol> {
  CheckConflictingMethods(final SymbolAndScopeManagement symbolAndScopeManagement,
                          final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public boolean test(final AggregateSymbol symbol) {

    final var result = new AtomicBoolean(true);

    //We only need to do this it there is a possibility of multiple implementations of a method signature.
    if (symbol instanceof AggregateWithTraitsSymbol aggregate) {

      //These are the methods that this aggregate has defined and some may override those in super or traits.
      final var concreteMethods = aggregate.getAllNonAbstractMethodsInThisScopeOnly();

      //This will get methods from the super and prune out any that match on this.
      final var nonOverriddenMethods = aggregate.getSuperAggregate().isPresent()
          ? new ArrayList<>(methodsNotDefinedInList(concreteMethods, aggregate.getSuperAggregate().get()))
          : new ArrayList<MethodSymbol>();

      symbol.getTraits().forEach(trait -> {
        //Again get the methods from this trait but prune out any that have been implemented.
        final var methodsToCheck = methodsNotDefinedInList(concreteMethods, trait);
        //Now build up the list or emit error if already in list - because that is the clash.
        methodsToCheck.forEach(method -> {
          final var acceptable = addToListOrError(aggregate, trait, nonOverriddenMethods, method);
          result.set(result.get() && acceptable);
        });
      });
    }

    return result.get();
  }

  private boolean addToListOrError(final IAggregateSymbol aggregate,
                                   final IAggregateSymbol trait,
                                   final List<MethodSymbol> nonOverriddenMethods,
                                   final MethodSymbol method) {

    final var possibleClashes = matchingMethods(nonOverriddenMethods, method);

    if (!possibleClashes.isEmpty()) {

      final var msg = "'" + method.getFriendlyName()
          + "' on '"
          + trait.getFriendlyName()
          + "' and '"
          + possibleClashes.get(0).getParentScope().getFriendlyScopeName()
          + "':";
      errorListener.semanticError(aggregate.getSourceToken(), msg, METHODS_CONFLICT);
      return false;
    }

    nonOverriddenMethods.add(method);

    return true;
  }

  private List<MethodSymbol> methodsNotDefinedInList(final List<MethodSymbol> defined,
                                                     final IAggregateSymbol aggregate) {

    return aggregate.getAllMethods().stream()
        .filter(method -> methodNotPresent(defined, method))
        .toList();

  }

  private boolean methodNotPresent(final List<MethodSymbol> defined, final MethodSymbol checkMethod) {

    return defined.stream()
        .noneMatch(method -> methodsMatch(method, checkMethod));

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
