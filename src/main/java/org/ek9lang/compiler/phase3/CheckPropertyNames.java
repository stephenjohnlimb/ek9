package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.DUPLICATE_PROPERTY_FIELD;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.LocationExtractor;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * As all properties on records are public, it is necessary to check they do not get
 * duplicated. In short in a record inheritance hierarchy the properties become flattened.
 * <br/>
 * This is unlike classes/components where all properties are always private and so can have
 * the same name.
 */
final class CheckPropertyNames extends RuleSupport implements Consumer<AggregateSymbol> {
  private final LocationExtractor locationExtractor = new LocationExtractor();

  CheckPropertyNames(SymbolAndScopeManagement symbolAndScopeManagement,
                     ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final AggregateSymbol aggregateSymbol) {

    //Only if it has a super, there won't be duplicates on the same aggregate.
    aggregateSymbol.getSuperAggregateSymbol().ifPresent(
        superAggregate -> aggregateSymbol.getProperties().forEach(
            property -> superAggregate.resolveMember(new SymbolSearch(property.getName())
            ).ifPresent(
                duplicateProperty -> emitDuplicatePropertyByName(aggregateSymbol, property, duplicateProperty)
            )
        )
    );
  }

  private void emitDuplicatePropertyByName(final AggregateSymbol aggregateSymbol,
                                           final ISymbol property,
                                           final ISymbol duplicateProperty) {
    var location = locationExtractor.apply(duplicateProperty);

    var msg = "Relating to '"
        + aggregateSymbol.getFriendlyName()
        + "' and '"
        + duplicateProperty.getFriendlyName()
        + "' "
        + location
        + ":";

    errorListener.semanticError(property.getSourceToken(), msg, DUPLICATE_PROPERTY_FIELD);
  }
}
