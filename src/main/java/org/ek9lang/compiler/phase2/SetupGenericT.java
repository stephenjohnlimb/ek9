package org.ek9lang.compiler.phase2;

import java.util.Optional;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.AggregateFactory;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;

/**
 * The Generic T used in parametric types will exist but won't have any methods on yet.
 * This is because it is only when we get to this stage can the possible constraining type
 * be resolved.
 * But also need to consider if it is a Function rather than an Aggregate.
 */
final class SetupGenericT implements Consumer<EK9Parser.ParameterisedDetailContext> {

  private final SupportsBeingConstrainingType supportsBeingConstrainingType = new SupportsBeingConstrainingType();
  private final SymbolsAndScopes symbolsAndScopes;
  private final AggregateFactory aggregateFactory;
  private final ErrorListener errorListener;

  SetupGenericT(final SymbolsAndScopes symbolsAndScopes,
                final AggregateFactory aggregateFactory,
                final ErrorListener errorListener) {

    this.symbolsAndScopes = symbolsAndScopes;
    this.aggregateFactory = aggregateFactory;
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.ParameterisedDetailContext ctx) {

    final var t = symbolsAndScopes.getRecordedSymbol(ctx);
    if (t instanceof AggregateSymbol aggregateT) {

      //So it is not constrained an is just a 'T'
      if (ctx.typeDef() == null) {
        aggregateFactory.addAllSyntheticOperators(aggregateT);
      } else {
        getConstrainingTypeOrError(ctx.typeDef())
            .ifPresent(constrainingType -> aggregateFactory.updateToConstrainBy(aggregateT, constrainingType));
      }
    }

  }

  private Optional<IAggregateSymbol> getConstrainingTypeOrError(final EK9Parser.TypeDefContext ctx) {

    final var theConstrainingType = symbolsAndScopes.getRecordedSymbol(ctx);

    if (theConstrainingType == null) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
    } else if (!supportsBeingConstrainingType.test(theConstrainingType)) {
      final var msg = "'"
          + theConstrainingType.getFriendlyName()
          + "' of '" + theConstrainingType.getCategory()
          + "/" + theConstrainingType.getGenus()
          + "':";
      errorListener.semanticError(ctx.start, msg,
          ErrorListener.SemanticClassification.CONSTRAINED_FUNCTIONS_NOT_SUPPORTED);
    } else {
      return Optional.of((IAggregateSymbol) theConstrainingType);
    }

    return Optional.empty();
  }
}
