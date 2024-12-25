package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_IMMEDIATE_TRAIT;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.TRAIT_ACCESS_NOT_SUPPORTED;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.LocationExtractorFromSymbol;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;

/**
 * Checks that a trait access like T1.xyz() - is allowed i.e. the 'T1' is a trait of the context.
 * Otherwise, it is not allowed and an error is emitted.
 */
class TraitAccessibleOrError extends TypedSymbolAccess implements BiConsumer<Token, ISymbol> {

  private final LocationExtractorFromSymbol locationExtractorFromSymbol = new LocationExtractorFromSymbol();

  TraitAccessibleOrError(final SymbolsAndScopes symbolsAndScopes,
                         final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final Token errorLocation, final ISymbol symbol) {
    //So now this trait can only be addressed like this is the context is in a class - that has that trait.
    if (symbol.getGenus().equals(SymbolGenus.CLASS_TRAIT) && symbol instanceof IAggregateSymbol asAggregate) {
      //First check if within a dynamic block, then a none block (class or dynamic class)
      final var possibleDynamicBlock = symbolsAndScopes.traverseBackUpStack(IScope.ScopeType.DYNAMIC_BLOCK);
      possibleDynamicBlock.ifPresentOrElse(
          dynamicBlock -> acceptableDynamicConstructUseOfTraitOrError(errorLocation, asAggregate, dynamicBlock),
          () -> {
            final var possibleNoneBlock = symbolsAndScopes.traverseBackUpStack(IScope.ScopeType.NON_BLOCK);
            possibleNoneBlock.ifPresent(noneBlock -> noneBlockUseOfTraitOrError(errorLocation, asAggregate, noneBlock));
          }
      );
    }
  }

  private void acceptableDynamicConstructUseOfTraitOrError(final Token errorLocation,
                                                           final IAggregateSymbol asAggregate,
                                                           final IScope dynamicBlock) {
    if (dynamicBlock instanceof AggregateWithTraitsSymbol asClass) {
      final var hasTrait = asClass.getTraits().contains(asAggregate);
      if (!hasTrait) {
        emitClassDoesNotHaveTrait(errorLocation, asAggregate, asClass);
      }
    } else {
      emitTraitNotAccessibleFromFunctionError(errorLocation, asAggregate);
    }
  }

  private void noneBlockUseOfTraitOrError(final Token errorLocation,
                                          final IAggregateSymbol asAggregate,
                                          final IScope noneBlock) {
    if (noneBlock instanceof AggregateWithTraitsSymbol asClass) {
      final var hasTrait = asClass.getTraits().contains(asAggregate);
      if (!hasTrait) {
        emitClassDoesNotHaveTrait(errorLocation, asAggregate, asClass);
      }
    } else {
      emitTraitNotAccessibleFromFunctionError(errorLocation, asAggregate);
    }
  }

  private void emitClassDoesNotHaveTrait(final Token errorLocation,
                                         final ISymbol symbol,
                                         final AggregateWithTraitsSymbol asClass) {
    final var msg = "wrt class as defined "
        + locationExtractorFromSymbol.apply(asClass)
        + " and trait "
        + symbol.getFriendlyName()
        + " as defined "
        + locationExtractorFromSymbol.apply(symbol)
        + ":";
    errorListener.semanticError(errorLocation, msg, NOT_IMMEDIATE_TRAIT);
  }

  private void emitTraitNotAccessibleFromFunctionError(final Token errorLocation,
                                                       final ISymbol symbol) {
    final var msg = "wrt "
        + symbol.getFriendlyName()
        + " as defined "
        + locationExtractorFromSymbol.apply(symbol)
        + ":";
    errorListener.semanticError(errorLocation, msg, TRAIT_ACCESS_NOT_SUPPORTED);
  }

}
