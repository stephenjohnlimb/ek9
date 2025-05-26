package org.ek9lang.compiler.phase2;

import java.util.Optional;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.AggregateManipulator;
import org.ek9lang.compiler.support.CommonValues;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;

/**
 * The Generic T used in parametric types will exist but won't have any methods on yet.
 * <p>
 * This is because it is only when we get to this stage can the possible constraining type
 * be resolved.
 * </p>
 * <p>
 * But also need to consider if it is a Function rather than an Aggregate.
 * </p>
 */
final class SetupGenericTOrError implements Consumer<EK9Parser.ParameterisedDetailContext> {

  private final SupportsBeingConstrainingType supportsBeingConstrainingType = new SupportsBeingConstrainingType();
  private final SymbolsAndScopes symbolsAndScopes;
  private final AggregateManipulator aggregateManipulator;
  private final ErrorListener errorListener;

  SetupGenericTOrError(final SymbolsAndScopes symbolsAndScopes,
                       final AggregateManipulator aggregateManipulator,
                       final ErrorListener errorListener) {

    this.symbolsAndScopes = symbolsAndScopes;
    this.aggregateManipulator = aggregateManipulator;
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.ParameterisedDetailContext ctx) {

    final var t = symbolsAndScopes.getRecordedSymbol(ctx);
    if (t instanceof AggregateSymbol aggregateT) {

      //So it is not constrained an is just a 'T'
      if (ctx.typeDef() == null) {
        aggregateManipulator.addAllSyntheticOperators(aggregateT);
        aggregateT.putSquirrelledData(CommonValues.CONSTRAIN, "FALSE");
      } else {
        getConstrainingTypeOrError(ctx.typeDef())
            .ifPresent(constrainingType -> {
              aggregateManipulator.updateToConstrainBy(aggregateT, constrainingType);
              aggregateT.putSquirrelledData(CommonValues.CONSTRAIN, "SUPER");
            });
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
