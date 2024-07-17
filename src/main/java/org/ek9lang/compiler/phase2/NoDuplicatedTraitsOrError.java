package org.ek9lang.compiler.phase2;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DUPLICATE_TRAIT_REFERENCE;

import java.util.HashSet;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;

/**
 * Just checks for duplicated trait names in the list.
 */
final class NoDuplicatedTraitsOrError implements Consumer<AggregateWithTraitsSymbol> {

  private final ErrorListener errorListener;

  NoDuplicatedTraitsOrError(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final AggregateWithTraitsSymbol aggregate) {

    final var traitNames = new HashSet<String>();

    aggregate.getTraits().forEach(trait -> {
      if (traitNames.contains(trait.getFullyQualifiedName())) {
        emitDuplicateTraitName(aggregate, trait);
      }
      traitNames.add(trait.getFullyQualifiedName());
    });

  }

  private void emitDuplicateTraitName(final IAggregateSymbol usedBy, final IAggregateSymbol trait) {

    errorListener.semanticError(usedBy.getSourceToken(),
        "'" + trait.getFriendlyName() + "'",
        DUPLICATE_TRAIT_REFERENCE);

  }
}
