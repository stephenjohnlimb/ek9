package org.ek9lang.compiler.phase3;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Designed to add in methods on the aggregate where the 'traits' use is 'traits by'.
 * What this means is any abstract methods in the trait (and its supers not implemented) must
 * have been implemented in the 'by {reference}'. Otherwise, there would have been errors when creating the
 * {reference}.
 * So what this means is that these same abstract methods must be 'overridden' in this aggregate, either by the
 * ek9 developer - or we have to add in a synthetic non-abstract method.
 * When it actually comes to actually implementing this code generated will actually add a real method in but delegate
 * the call to the {reference} object.
 */
final class AugmentAggregateWithTraitMethods extends TypedSymbolAccess
    implements BiConsumer<EK9Parser.TraitsListContext, AggregateWithTraitsSymbol> {

  private final TraverseAbstractMethods traverseAbstractMethods = new TraverseAbstractMethods();

  AugmentAggregateWithTraitMethods(final SymbolsAndScopes symbolsAndScopes,
                                   final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.TraitsListContext traitsListContext,
                     final AggregateWithTraitsSymbol aggregate) {

    if (traitsListContext == null || aggregate == null) {
      return;
    }

    //Only those traits that have been marked with 'by identifier' are candidates.
    traitsListContext.traitReference().stream()
        .filter(traitCtx -> traitCtx.identifier() != null)
        .forEach(traitCtx -> augmentAsAppropriate(traitCtx, aggregate));

  }

  private void augmentAsAppropriate(final EK9Parser.TraitReferenceContext traitCtx,
                                    final AggregateWithTraitsSymbol aggregate) {

    final var symbol = getRecordedAndTypedSymbol(traitCtx.identifierReference());
    if (symbol instanceof AggregateWithTraitsSymbol trait) {
      Consumer<MethodSymbol> actionToTake = match -> {
        if (match.isMarkedAbstract()) {
          cloneMethodToAggregate(traitCtx, match, aggregate);
        }
      };
      //You may think this pointless, but a class may have some traits without 'by'
      traverseAbstractMethods.accept(trait, actionToTake);
    }

  }

  private void cloneMethodToAggregate(final EK9Parser.TraitReferenceContext traitCtx,
                                      final MethodSymbol method,
                                      final AggregateWithTraitsSymbol aggregate) {

    final var token = new Ek9Token(traitCtx.start);
    final var newMethod = method.clone(aggregate);

    //Here we are ensuring that the class using this can call it
    newMethod.setOverride(true);
    newMethod.setMarkedAbstract(false);
    newMethod.setSynthetic(true);
    newMethod.setSourceToken(token);

    //But note we need to ensure that if there are any return values they are marked as
    //initialised. The real delegate will have been checked in this way, before it can be used.
    if (newMethod.isReturningSymbolPresent()) {
      newMethod.getReturningSymbol().setSourceToken(token);
      newMethod.getReturningSymbol().setInitialisedBy(token);
    }
    aggregate.define(newMethod);

  }
}
